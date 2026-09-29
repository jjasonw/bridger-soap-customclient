package com.lexisnexis.bridger.util;

import com.lexisnexis.bridger.client.BridgerSoapClientApp121;
import com.lexisnexis.bridger.generated.*;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * CSV导出工具类
 * 
 * 该类负责将Bridger API的请求和响应数据导出为CSV格式文件。
 * CSV文件包含详细的请求信息和响应信息，便于后续分析和处理。
 * 
 * CSV文件结构设计：
 * - 包含完整的请求字段（ClientID, UserID, 姓名, 地址等）
 * - 包含完整的响应字段（ResultID, 实体信息, 匹配结果等）
 * - 支持后期字段扩展
 * - 支持批量结果导出
 * 
 * @author Bridger SOAP Client Generator
 * @version 2.0.0
 */
public class CsvExporter {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvExporter.class);
    
    /**
     * CSV文件头部定义
     * 包含请求信息字段和响应信息字段
     */
    private static final String[] CSV_HEADER = {
        // 请求信息字段
        "Req_ClientID",
        "Req_UserID",
        "Req_ClientReference",
        "Req_EntityType",
        "Req_FirstName",
        "Req_MiddleName",
        "Req_LastName",
        "Req_FullName",
        "Req_Street",
        "Req_City",
        "Req_State",
        "Req_Country",
        "Req_PostalCode",
        "Req_DOB",
        "Req_Citizenship",
        
        // 响应信息字段
        "Res_Status",
        "Res_ResultID",
        "Res_RunID",
        "Res_EntityType",
        "Res_FullName",
        "Res_FirstName",
        "Res_MiddleName",
        "Res_LastName",
        "Res_ErrorMessage",
        "Res_MatchID",
        "EntityUniqueID",
        "EntityScore",
        "EntityName",
        "BestNameScore",
        "BestName",
        "BestDOBIsPartial",
        "BestCountryType",
        "BestCountryScore",
        "BestCountry",
        "BestAddressIsPartial",
        "ERF_Guidance",
        "ERF_ModelType",
        "ERF_SI", //StrengthIndex
        "ERF_LS" , //LocationScore
        "ERF_MIS", //MiddleNameInitialScore
        "ERF_DBS", //DateOfBirthScore
        "ERF_BP", //BurdenOfProofScore
        "ERF_IMS", //IdentifierMatchingScore
        "ERF_ARS", //AdditionalRuleScore
        "ERF_NFS", //NameFrequencyScore
        "ERF_NQS" //NameQualityScore
    };
    
    /**
     * 批量结果数据类
     * 用于在BridgerSoapClientApp和CsvExporter之间传递数据
     */
    public static class BatchResult {
        public Search request;
        public SearchResults response;
        public Exception error;
        
        public BatchResult(Search request, SearchResults response, Exception error) {
            this.request = request;
            this.response = response;
            this.error = error;
        }
    }
    
    /**
     * 导出批量搜索结果到CSV文件
     * 
     * 该方法接受一个批量结果列表，将每个请求及其对应的响应（可能包含多条记录）
     * 导出到CSV文件中。如果响应包含多条记录，每条记录占一行。
     * 
     * @param outputPath 输出CSV文件路径
     * @param results 批量结果列表（包含请求、响应和错误信息）
     * @throws IOException 如果文件写入失败
     */
    public static void exportBatchResults(Path outputPath, List<?> results) 
            throws IOException {
        
        logger.info("开始导出批量结果到CSV文件: {}", outputPath);
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toFile()))) {
            
            // 写入CSV头部
            writer.writeNext(CSV_HEADER);
            
            int totalRows = 0;
            
            // 处理每个搜索结果
            for (Object resultObj : results) {
                // 使用反射获取字段（因为BridgerSoapClientApp.SearchResult是内部类）
                try {

                    Search request = ((BridgerSoapClientApp121.SearchResult)resultObj).getSearch();
                    SearchResults response = ((BridgerSoapClientApp121.SearchResult)resultObj).getResponse();
                    Exception error = ((BridgerSoapClientApp121.SearchResult)resultObj).getError();
                    //Search request = (Search) resultObj.getClass().getField("request").get(resultObj);
                    //SearchResults response = (SearchResults) resultObj.getClass().getField("response").get(resultObj);
                    //Exception error = (Exception) resultObj.getClass().getField("error").get(resultObj);
                    
                    // 提取请求信息
                    RequestInfo requestInfo = extractRequestInfo(request);
                    
                    if (error != null) {
                        // 如果有错误，写入一行错误信息
                        String[] row = buildErrorRow(requestInfo, error);
                        writer.writeNext(row);
                        totalRows++;
                        
                    } else if (response != null && 
                               response.getRecords() != null && 
                               response.getRecords().getResultRecord() != null) {
                        
                        List<ResultRecord> records = response.getRecords().getResultRecord();
                        
                        if (records.isEmpty()) {
                            // 如果没有匹配记录，写入一行空响应
                            String[] row = buildNoResultRow(requestInfo);
                            writer.writeNext(row);
                            totalRows++;
                        } else {
                            // 为每条响应记录写入一行, AlertInfo, MatchInfo, ERFInfo
                            for (ResultRecord record : records) {
                                if(record.getWatchlist() != null && record.getWatchlist().getMatches()!= null
                                && record.getWatchlist().getMatches().getWLMatch() != null){
                                    for(WLMatch wlMatch : record.getWatchlist().getMatches().getWLMatch()){
                                        String[] row = buildCsvRow(requestInfo, record, wlMatch);
                                        writer.writeNext(row);
                                        totalRows++;
                                    }
                                }
                            }
                        }
                    } else {
                        // 响应为空
                        String[] row = buildNoResultRow(requestInfo);
                        writer.writeNext(row);
                        totalRows++;
                    }
                    
                } catch (Exception e) {
                    logger.error("处理结果对象失败: {}", e.getMessage(), e);
                }
            }
            
            logger.info("CSV导出完成，共写入 {} 行数据", totalRows);
        }
    }
    
    /**
     * 导出单个搜索结果到CSV文件（向后兼容）
     * 
     * @param outputPath 输出CSV文件路径
     * @param searchRequest 搜索请求对象
     * @param searchResponse 搜索响应对象
     * @throws IOException 如果文件写入失败
     */
    public static void exportToCSV(Path outputPath, Search searchRequest, SearchResults searchResponse) 
            throws IOException {
        
        logger.info("开始导出CSV文件: {}", outputPath);
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toFile()))) {
            
            // 写入CSV头部
            writer.writeNext(CSV_HEADER);
            
            // 提取请求信息
            RequestInfo requestInfo = extractRequestInfo(searchRequest);
            
            // 处理响应中的每条记录
            if (searchResponse != null && 
                searchResponse.getRecords() != null && 
                searchResponse.getRecords().getResultRecord() != null) {
                
                List<ResultRecord> records = searchResponse.getRecords().getResultRecord();
                
                logger.info("处理 {} 条响应记录", records.size());
                
                for (ResultRecord record : records) {

                    if(record.getWatchlist() != null && record.getWatchlist().getMatches()!= null
                            && record.getWatchlist().getMatches().getWLMatch() != null){
                        for(WLMatch wlMatch : record.getWatchlist().getMatches().getWLMatch()){
                            String[] row = buildCsvRow(requestInfo, record, wlMatch);
                            writer.writeNext(row);

                        }
                    }
//                    String[] row = buildCsvRow(requestInfo, record);
//                    writer.writeNext(row);
                }
            } else {
                // 如果没有响应记录，至少写入请求信息
                String[] row = buildNoResultRow(requestInfo);
                writer.writeNext(row);
            }
            
            logger.info("CSV导出完成");
        }
    }
    
    /**
     * 从Search请求中提取请求信息
     * 
     * @param searchRequest 搜索请求对象
     * @return 请求信息对象
     */
    private static RequestInfo extractRequestInfo(Search searchRequest) {
        RequestInfo info = new RequestInfo();
        
        if (searchRequest == null) {
            return info;
        }
        
        // 提取Context信息
        if (searchRequest.getContext() != null) {
            ClientContext context = searchRequest.getContext();
            info.clientId = safeString(context.getClientID());
            info.userId = safeString(context.getUserID());
        }
        
        // 提取ClientReference从 Context
        if (searchRequest.getContext() != null) {
            info.clientReference = safeString(searchRequest.getContext().getClientReference());
        }
        
        // 提取Input信息
        if (searchRequest.getInput() != null && 
            searchRequest.getInput().getRecords() != null &&
            searchRequest.getInput().getRecords().getInputRecord() != null &&
            !searchRequest.getInput().getRecords().getInputRecord().isEmpty()) {
            
            // 获取第一条记录
            InputRecord inputRecord = searchRequest.getInput().getRecords().getInputRecord().get(0);
            if (inputRecord == null || inputRecord.getEntity() == null) {
                return info;
            }
            
            InputEntity entity = inputRecord.getEntity();
            
            // 提取实体类型
            if (entity.getEntityType() != null) {
                info.entityType = entity.getEntityType().value();
            }
            
            // 提取姓名信息
            if (entity.getName() != null) {
                InputName name = entity.getName();
                info.firstName = safeString(name.getFirst());
                info.middleName = safeString(name.getMiddle());
                info.lastName = safeString(name.getLast());
                info.fullName = safeString(name.getFull());
            }
            
            // 提取地址信息（取第一个地址）
            if (entity.getAddresses() != null && 
                entity.getAddresses().getInputAddress() != null &&
                !entity.getAddresses().getInputAddress().isEmpty()) {
                
                InputAddress address = entity.getAddresses().getInputAddress().get(0);
                info.street = safeString(address.getStreet1());
                info.city = safeString(address.getCity());
                info.state = safeString(address.getStateProvinceDistrict());
                info.country = safeString(address.getCountry());
                info.postalCode = safeString(address.getPostalCode());
            }
            
            // 提取出生日期和国籍（从 AdditionalInfo 中）
            if (entity.getAdditionalInfo() != null && 
                entity.getAdditionalInfo().getInputAdditionalInfo() != null) {
                for (InputAdditionalInfo addInfo : entity.getAdditionalInfo().getInputAdditionalInfo()) {
                    if (addInfo.getType() != null) {
                        String type = addInfo.getType().value();
                        if ("DateOfBirth".equalsIgnoreCase(type)) {
                            info.dob = safeString(addInfo.getValue());
                        } else if ("Citizenship".equalsIgnoreCase(type)) {
                            info.citizenship = safeString(addInfo.getValue());
                        }
                    }
                }
            }
        }
        
        return info;
    }
    
    /**
     * 构建CSV数据行（包含请求和响应信息）
     * 
     * @param requestInfo 请求信息
     * @param record 响应记录
     * @return CSV数据行
     */
    private static String[] buildCsvRow(RequestInfo requestInfo, ResultRecord record, WLMatch wlMatch) {
        List<String> row = new ArrayList<>();
        
        // 添加请求信息
        row.add(requestInfo.clientId);
        row.add(requestInfo.userId);
        row.add(requestInfo.clientReference);
        row.add(requestInfo.entityType);
        row.add(requestInfo.firstName);
        row.add(requestInfo.middleName);
        row.add(requestInfo.lastName);
        row.add(requestInfo.fullName);
        row.add(requestInfo.street);
        row.add(requestInfo.city);
        row.add(requestInfo.state);
        row.add(requestInfo.country);
        row.add(requestInfo.postalCode);
        row.add(requestInfo.dob);
        row.add(requestInfo.citizenship);
        
        // 添加响应信息
        row.add("SUCCESS"); // Response_Status
        row.add(safeString(record.getResultID())); // Response_ResultID
        row.add(safeString(record.getRunID())); // Response_RecordNumber

        // 提取响应详细信息
        if (record.getRecordDetails() != null) {
            ResultRecordDetails details = record.getRecordDetails();
            
            // 实体类型
            row.add(details.getEntityType() != null ? details.getEntityType().value() : "");
            
            // 姓名信息
            if (details.getName() != null) {
                InputName name = details.getName();
                row.add(safeString(name.getFull()));
                row.add(safeString(name.getFirst()));
                row.add(safeString(name.getMiddle()));
                row.add(safeString(name.getLast()));
            } else {
                row.add(""); // FullName
                row.add(""); // FirstName
                row.add(""); // MiddleName
                row.add(""); // LastName
            }
            
            // 地址信息（取第一个地址）
//            if (details.getAddresses() != null &&
//                details.getAddresses().getInputAddress() != null &&
//                !details.getAddresses().getInputAddress().isEmpty()) {
//
//                InputAddress address = details.getAddresses().getInputAddress().get(0);
//                row.add(safeString(address.getStreet1()));
//                row.add(safeString(address.getCity()));
//                row.add(safeString(address.getStateProvinceDistrict()));
//                row.add(safeString(address.getCountry()));
//                row.add(safeString(address.getPostalCode()));
//            } else {
//                row.add(""); // Street
//                row.add(""); // City
//                row.add(""); // State
//                row.add(""); // Country
//                row.add(""); // PostalCode
//            }
            
            // 出生日期和国籍（从 AdditionalInfo 中提取）
//            String dob = "";
//            String citizenship = "";
//            if (details.getAdditionalInfo() != null &&
//                details.getAdditionalInfo().getInputAdditionalInfo() != null) {
//                for (InputAdditionalInfo addInfo : details.getAdditionalInfo().getInputAdditionalInfo()) {
//                    if (addInfo.getType() != null) {
//                        String type = addInfo.getType().value();
//                        if ("DateOfBirth".equalsIgnoreCase(type)) {
//                            dob = safeString(addInfo.getValue());
//                        } else if ("Citizenship".equalsIgnoreCase(type)) {
//                            citizenship = safeString(addInfo.getValue());
//                        }
//                    }
//                }
//            }
//            row.add(dob);
//            row.add(citizenship);
            
            // LexID 和 Account Number（从IDs中提取）
//            String lexId = "";
//            String accountNumber = "";
//            if (details.getIDs() != null &&
//                details.getIDs().getInputID() != null) {
//                for (InputID id : details.getIDs().getInputID()) {
//                    if (id.getType() != null) {
//                        String idType = id.getType().value(); // InputID.getType() 返回IDType枚举
//                        if ("LexID".equalsIgnoreCase(idType)) {
//                            lexId = safeString(id.getNumber());
//                        } else if ("Account".equalsIgnoreCase(idType)) {
//                            accountNumber = safeString(id.getNumber());
//                        }
//                    }
//                }
//            }
//            row.add(lexId);
//            row.add(accountNumber);
            
        } else {
            // 如果没有详细信息，填充空值
            for (int i = 0; i < 6; i++) { //多少响应信息，除了前三个及ErrorMessage
                row.add("");
            }
        }
        
        // 错误信息（成功时为空）
        row.add("");

        if(wlMatch != null){
            //MatchID
            row.add(safeString(wlMatch.getID()));
            //"EntityUniqueID"
            row.add(safeString(wlMatch.getEntityUniqueID()));
            //"EntityScore"
            row.add(safeString(wlMatch.getEntityScore()));
            //"EntityName"
            row.add(safeString(wlMatch.getEntityName()));
            //"BestNameScore"
            row.add(safeString(wlMatch.getBestNameScore()));
            //"BestName"
            row.add(safeString(wlMatch.getBestName()));
            //"BestDOBIsPartial"
            //row.add(safeString(wlMatch.getBestDOBIsPartial()));
            //"BestCountryType"
            row.add(safeString(wlMatch.getBestCountryType()));
            //"BestCountryScore"
            row.add(safeString(wlMatch.getBestCountryScore()));
            //"BestCountry"
            row.add(safeString(wlMatch.getBestCountry()));
            //"BestAddressIsPartial"
            //row.add(safeString(wlMatch.getBestAddressIsPartial()));

            //ERF Details
            WLMatchErfDetails erfDetails = wlMatch.getErfDetails();
            if(erfDetails != null){
                //ERF_Guidance
                row.add(safeString(erfDetails.getGuidance()));
                // ERF_ModelType -> use enum value() to match WSDL string (e.g. "None", "PEP")
                row.add(erfDetails.getModelType() != null ? erfDetails.getModelType().value() : "");
                // ERF_StrengthIndex -> strengthIndex is int in generated model
                row.add(String.valueOf(erfDetails.getStrengthIndex()));
                 // --- Build name->value map and write columns in fixed order ---
                 Map<String, String> erfMap = new HashMap<>();
                 if (erfDetails.getAdditionalInfo() != null && erfDetails.getAdditionalInfo().getErfAdditionalInfo() != null) {
                     for (ErfAdditionalInfo info : erfDetails.getAdditionalInfo().getErfAdditionalInfo()) {
                         if (info != null && info.getName() != null) {
                             erfMap.put(info.getName(), info.getValue() != null ? info.getValue() : "");
                         }
                     }
                 }
                // Debug: if additional info map is empty, log information to help diagnose
                if (erfMap.isEmpty()) {
                    logger.debug("WLMatch id={} has ErfDetails but no AdditionalInfo/ErfAdditionalInfo elements", wlMatch.getID());
                } else {
                    logger.debug("WLMatch id={} ErfAdditionalInfo keys={} values={}", wlMatch.getID(), erfMap.keySet(), erfMap);
                }

                 // Fixed order of ERF additional fields matching CSV_HEADER
                 String locationScore = safeString(erfMap.get("LocationScore"));
                 String middleNameInitialScore = safeString(erfMap.get("MiddleNameInitialScore"));
                 String dateOfBirthScore = safeString(erfMap.get("DateOfBirthScore"));
                 String burdenOfProofScore = safeString(erfMap.get("BurdenOfProofScore"));
                 String identifierMatchingScore = safeString(erfMap.get("IdentifierMatchingScore"));
                 String additionalRuleScore = safeString(erfMap.get("AdditionalRuleScore"));
                 String nameFrequencyScore = safeString(erfMap.get("NameFrequencyScore"));
                 String nameQualityScore = safeString(erfMap.get("NameQualityScore"));

                 // Append in exact header order (ERF_LS, ERF_MIS, ERF_DBS, ERF_BP, ERF_IMS, ERF_ARS, ERF_NFS, ERF_NQS)
                 row.add(locationScore);
                 row.add(middleNameInitialScore);
                 row.add(dateOfBirthScore);
                 row.add(burdenOfProofScore);
                 row.add(identifierMatchingScore);
                 row.add(additionalRuleScore);
                 row.add(nameFrequencyScore);
                 row.add(nameQualityScore);

             } else {
                logger.debug("WLMatch id={} has no ErfDetails", wlMatch.getID());
                 // 如果没有ERF信息，填充空值 for ERF_Guidance/ModelType/SI + 8 additional fields
                 // Note: Guidance/ModelType/SI already added above; fill remaining 8 fields
                 for (int i = 0; i < 8; i++) {
                     row.add("");
                 }
             }
         } else {
             // 如果没有MatchLevel信息，填充空值 (Res_MatchID + ERF fields)
             for (int i = 0; i < 19; i++) { // Res_MatchID + ERF_Guidance + ERF_ModelType + ERF_SI + 8 additional
                 row.add("");
             }
         }


        
        return row.toArray(new String[0]);
    }
    
    /**
     * 构建无结果的CSV数据行
     * 
     * @param requestInfo 请求信息
     * @return CSV数据行
     */
    private static String[] buildNoResultRow(RequestInfo requestInfo) {
        List<String> row = new ArrayList<>();
        
        // 添加请求信息
        row.add(requestInfo.clientId);
        row.add(requestInfo.userId);
        row.add(requestInfo.clientReference);
        row.add(requestInfo.entityType);
        row.add(requestInfo.firstName);
        row.add(requestInfo.middleName);
        row.add(requestInfo.lastName);
        row.add(requestInfo.fullName);
        row.add(requestInfo.street);
        row.add(requestInfo.city);
        row.add(requestInfo.state);
        row.add(requestInfo.country);
        row.add(requestInfo.postalCode);
        row.add(requestInfo.dob);
        row.add(requestInfo.citizenship);
        
        // 响应状态
        row.add("NO_RESULTS");
        
        // 其余响应字段为空
        for (int i = 0; i < 21; i++) {
            row.add("");
        }
        
        return row.toArray(new String[0]);
    }
    
    /**
     * 构建错误的CSV数据行
     * 
     * @param requestInfo 请求信息
     * @param error 错误对象
     * @return CSV数据行
     */
    private static String[] buildErrorRow(RequestInfo requestInfo, Exception error) {
        List<String> row = new ArrayList<>();
        
        // 添加请求信息
        row.add(requestInfo.clientId);
        row.add(requestInfo.userId);
        row.add(requestInfo.clientReference);
        row.add(requestInfo.entityType);
        row.add(requestInfo.firstName);
        row.add(requestInfo.middleName);
        row.add(requestInfo.lastName);
        row.add(requestInfo.fullName);
        row.add(requestInfo.street);
        row.add(requestInfo.city);
        row.add(requestInfo.state);
        row.add(requestInfo.country);
        row.add(requestInfo.postalCode);
        row.add(requestInfo.dob);
        row.add(requestInfo.citizenship);
        
        // 响应状态
        row.add("ERROR");
        
        // 其余响应字段为空（除了最后的错误信息）
        for (int i = 0; i < 20; i++) {
            row.add("");
        }
        
        // 错误信息
        row.add(error.getMessage());
        
        return row.toArray(new String[0]);
    }
    
    /**
     * 安全转换为字符串
     * 如果对象为null，返回空字符串
     * 
     * @param obj 要转换的对象
     * @return 字符串表示
     */
    private static String safeString(Object obj) {
        return obj != null ? obj.toString() : "";
    }
    
    /**
     * 请求信息内部类
     * 用于存储从Search请求中提取的信息
     */
    private static class RequestInfo {
        String clientId = "";
        String userId = "";
        String clientReference = "";
        String entityType = "";
        String firstName = "";
        String middleName = "";
        String lastName = "";
        String fullName = "";
        String street = "";
        String city = "";
        String state = "";
        String country = "";
        String postalCode = "";
        String dob = "";
        String citizenship = "";
    }
}
