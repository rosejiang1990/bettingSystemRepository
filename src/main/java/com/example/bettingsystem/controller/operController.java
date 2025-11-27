package com.example.bettingsystem.controller;
import com.example.bettingsystem.util.RandomUtil;
import com.example.bettingsystem.pojo.BettingPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Slf4j
@RestController
public class operController {
    @Autowired
    private HttpSession session;
    private Map<String, Map<String, List<BigDecimal>>> map = new HashMap<>();

    @GetMapping("/{customerid}/session")
    public String getStsToken(@PathVariable String customerid) {
        if(StringUtils.isEmpty(customerid)){
            throw new RuntimeException("customerid is not null");
        }
        log.info("getStsToken  customerid = {}", customerid);
        //Retrieve token from session by customerID
        String token = (String)session.getAttribute(customerid);
        if(StringUtils.isEmpty(token)){
            //If the token does not exist, regenerate it and set the expiration time to 10 minutes
            synchronized(this){
                token = RandomUtil.randomToken();
                session.setAttribute(customerid,token);
                session.setMaxInactiveInterval(10*60);
            }
        }
        return token;
    }

    @PostMapping("/{betoferid}/stake")
    public String stake(@PathVariable String betoferid, @RequestParam String sessionKey,
                           @RequestBody BettingPO bettingPO) throws JsonProcessingException {
        if(StringUtils.isEmpty(betoferid)){
            throw new RuntimeException("betoferid is not null");
        }
        if(StringUtils.isEmpty(sessionKey)){
            throw new RuntimeException("sessionKey is not null");
        }
        if(null == bettingPO){
            throw new RuntimeException("bettingPO is not null");
        }
        log.info("stake betoferid = {}, sessionKey={}",betoferid, sessionKey);
        //Check if the sessionKey exists in the session; if not, display an error message
        Boolean isCheck = Boolean.FALSE;
        String customerId = "";
        Enumeration<String> attrs = session.getAttributeNames();
        while(attrs.hasMoreElements()){
            String name = attrs.nextElement().toString();
            String value = (String)session.getAttribute(name);
            if(value.equals(sessionKey)){
                isCheck = Boolean.TRUE;
                customerId = name;
            }
        }
        if(!isCheck){
            throw new RuntimeException("session is timeOut or not exit");
        }
        //Retrieve betting promotions by customer ID; if the current bet exists, directly add the bet amount;
        //otherwise, create a new bet item and bet amount
        synchronized(this){
            Map<String, List<BigDecimal>> custmpa = map.get(customerId);
            if(null != custmpa){
                List<BigDecimal> decimalList = custmpa.get(betoferid);
                if(null != decimalList && decimalList.size() > 0){
                    decimalList.add(bettingPO.getAmount());
                }else{
                    List<BigDecimal> bigDecimals = new ArrayList<>();
                    bigDecimals.add(bettingPO.getAmount());
                    custmpa.put(betoferid,bigDecimals);
                }
            }else{
                List<BigDecimal> bigDecimals = new ArrayList<>();
                bigDecimals.add(bettingPO.getAmount());
                Map<String, List<BigDecimal>> amoutMap = new HashMap<>();
                amoutMap.put(betoferid,bigDecimals);
                map.put(customerId,amoutMap);
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(map);
        log.info("{}",jsonString);
        return "success";
    }

    @GetMapping("/{betoferid}/highstakes")
    public String highstakes(@PathVariable String betoferid) throws JsonProcessingException {
        if(StringUtils.isEmpty(betoferid)){
            throw new RuntimeException("betoferid is not null");
        }
        //Iterate through the bet amount of each user's current betting item and retrieve the maximum amount
        String result= "";
        Map<String, BigDecimal> betoferMap = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String,List<BigDecimal>>> entry : map.entrySet()) {
            List<BigDecimal> bigDecimalList = entry.getValue().get(betoferid);
            BigDecimal sortValue = null;
            if(betoferMap.get(entry.getKey()) != null){
                sortValue = sortList(betoferMap.get(entry.getKey()),bigDecimalList);
            }else{
                sortValue = sortList(null,bigDecimalList);
            }
            if(null != sortValue){
                betoferMap.put(entry.getKey(),sortValue);
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(betoferMap);
        log.info("{}",jsonString);
        //Sort the betting amounts corresponding to each customer, take the top 20, and return them
        Map<String, BigDecimal> sortedByValueDesc = betoferMap.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new));
        int hashSize = 2;
        if(null == sortedByValueDesc || sortedByValueDesc.size() == 0){
            hashSize = 0;
        }
        if(null != sortedByValueDesc && sortedByValueDesc.size() < 2){
            hashSize = sortedByValueDesc.size();
        }
        int count = 0;
        Iterator<Map.Entry<String, BigDecimal>> iterator = sortedByValueDesc.entrySet().iterator();
        StringBuffer resultStr = new StringBuffer();
        while (iterator.hasNext() && count < hashSize) {
            Map.Entry<String, BigDecimal> entry = iterator.next();
            resultStr.append(","+entry.getKey()+"="+entry.getValue());
            count++;
        }
        if(resultStr.length() > 0){
            result = resultStr.substring(1);
        }
        return result;
    }

    private BigDecimal sortList(BigDecimal oldValue, List<BigDecimal> twoList){
        BigDecimal result = new BigDecimal(0);
        List<BigDecimal> dataList = new ArrayList<>();
        if(null != oldValue ){
            dataList.add(oldValue);
        }
        if(null != twoList && twoList.size() > 0){
            dataList.addAll(twoList);
        }
        Collections.sort(dataList, (o1, o2) -> o2.compareTo(o1));
        if(null != dataList && dataList.size() > 0){
            if(oldValue == null || dataList.get(0).compareTo(oldValue) > 0){
                result = dataList.get(0);
            }else{
                result = oldValue;
            }
        }else{
            result = oldValue;
        }
        return result;
    }
}
