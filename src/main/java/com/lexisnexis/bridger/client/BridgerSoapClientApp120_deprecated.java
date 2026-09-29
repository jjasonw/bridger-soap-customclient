package com.lexisnexis.bridger.client;

import com.lexisnexis.bridger.generated.*;
import com.lexisnexis.bridger.util.ApplicationConfiguration;
import com.lexisnexis.bridger.service.SearchRequestBuilder;
import com.lexisnexis.bridger.util.CsvExporter;
import com.lexisnexis.bridger.util.CsvInputReader;
import com.lexisnexis.bridger.util.XmlFormatter;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridger SOAP客户端主应用程序
 * 
 * 本类实现了从input.csv文件读取搜索请求数据，批量调用Bridger Insight API，
 * 并将所有响应结果写入到带时间戳的output_YYYYMMdd_HH.MM.sss.csv文件中。
 * 
 * 主要功能：
 * 1. 从input.csv读取搜索请求数据
 * 2. 为每条记录构建Search请求
 * 3. 批量调用Bridger API
 * 4. 将所有结果导出到CSV文件
 * 5. 提供详细的日志记录
 * 
 * 使用示例：
 * <pre>
 * // 在项目根目录创建input.csv文件
 * // 运行程序
 * BridgerSoapClientApp.main(new String[]{});
 * // 查看生成的output_YYYYMMdd_HH.MM.sss.csv文件
 * </pre>
 * 
 * @author Bridger SOAP Client Team
 * @version 2.0.0
 */
public class BridgerSoapClientApp120_deprecated {
    
    private static final Logger logger = LoggerFactory.getLogger(BridgerSoapClientApp120_deprecated.class);
    
    // ==================== API凭据配置 ====================
    // 注意：请替换为实际的API凭据，不要将真实凭据提交到版本控制系统
    private static final String CLIENT_ID = ApplicationConfiguration.getRequiredProperty("bridger.api.clientId");
    private static final String USER_ID = ApplicationConfiguration.getRequiredProperty("bridger.api.userId");
    private static final String PASSWORD = ApplicationConfiguration.getRequiredProperty("bridger.api.password");
    
    // ==================== 文件路径配置 ====================
    private static final String INPUT_CSV_PATH = "input.csv";
    
    // ==================== 批量处理配置 ====================
    // 请求之间的延迟时间（毫秒），避免API限流
    private static final long REQUEST_DELAY_MS = 1000;
    
    /**
     * CSV输入数据模型
     * 用于存储从input.csv读取的每一行数据
     */
    private static class InputRecord {
        String clientReference;
        String entityType;
        String firstName;
        String middleName;
        String lastName;
        String fullName;
        String street;
        String city;
        String state;
        String country;
        String postalCode;
        String dob;
        String citizenship;
        String idType;
        String idNumber;
    }
    
    /**
     * 搜索结果模型
     * 用于存储每次API调用的请求和响应
     */
    public static class SearchResult {
        protected Search request;
        protected SearchResults response;
        protected Exception error;
        
        SearchResult(Search request, SearchResults response) {
            this.request = request;
            this.response = response;
        }
        
        SearchResult(Search request, Exception error) {
            this.request = request;
            this.error = error;
        }

        public Search getSearch() {
            return this.request;
        }

        public SearchResults getResponse() {
            return this.response;
        }

        public Exception getError() {
            return this.error;
        }
    }
    
    /**
     * 应用程序主入口
     * 
     * @param args 命令行参数（当前未使用）
     */
    public static void main(String[] args) {
        logger.info("=== Bridger SOAP Client 应用程序启动 ===");
        logger.info("版本: 2.0.0");
        logger.info("模式: 批量CSV处理");
        
        try {
            // 步骤1: 读取input.csv文件
            logger.info("步骤1: 读取input.csv文件");
            List<InputRecord> inputRecords = readInputCsv(INPUT_CSV_PATH);
            logger.info("成功读取 {} 条输入记录", inputRecords.size());
            
            if (inputRecords.isEmpty()) {
                logger.warn("input.csv文件为空或不包含有效数据，程序退出");
                return;
            }
            
            // 步骤2: 初始化SOAP客户端
            logger.info("步骤2: 初始化SOAP客户端");
            BridgerSoapClient client = new BridgerSoapClient();
            logger.info("SOAP客户端初始化完成");
            
            // 步骤3: 批量处理搜索请求
            logger.info("步骤3: 批量处理搜索请求");
            List<SearchResult> results = processBatchSearch(client, inputRecords);
            logger.info("批量搜索完成，成功: {}, 失败: {}", 
                countSuccessful(results), countFailed(results));
            
            // 步骤4: 导出结果到CSV
            logger.info("步骤4: 导出结果到CSV文件");
            String outputFileName = generateOutputFileName();
            Path outputPath = Paths.get(outputFileName);
            exportResults(results, outputPath);
            logger.info("结果已导出到: {}", outputPath.toAbsolutePath());
            
            // 步骤5: 打印统计信息
            logger.info("步骤5: 打印统计信息");
            printStatistics(results);
            
            logger.info("=== 应用程序执行完成 ===");
            
        } catch (IOException e) {
            logger.error("文件读写错误: {}", e.getMessage(), e);
        } catch (CsvException e) {
            logger.error("CSV解析错误: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("应用程序执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 从CSV文件读取输入数据
     * 
     * @param filePath CSV文件路径
     * @return 输入记录列表
     * @throws IOException 如果文件读取失败
     * @throws CsvException 如果CSV解析失败
     */
    private static List<InputRecord> readInputCsv(String filePath) 
            throws IOException, CsvException {
        
        List<InputRecord> records = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(CsvInputReader.open(filePath))) {
            List<String[]> rows = reader.readAll();
            
            if (rows.isEmpty()) {
                logger.warn("CSV文件为空");
                return records;
            }
            
            // 跳过头部行
            String[] headers = rows.get(0);
            logger.debug("CSV头部: {}", String.join(", ", headers));
            
            // 处理数据行
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                
                // 跳过空行
                if (row.length == 0 || (row.length == 1 && row[0].trim().isEmpty())) {
                    continue;
                }
                
                try {
                    InputRecord record = parseInputRecord(row);
                    records.add(record);
                    logger.debug("读取记录 {}: {}", i, record.clientReference);
                } catch (Exception e) {
                    logger.error("解析第 {} 行数据失败: {}", i + 1, e.getMessage());
                }
            }
        }
        
        return records;
    }
    
    /**
     * 解析CSV行数据为InputRecord对象
     * 
     * @param row CSV行数据
     * @return InputRecord对象
     */
    private static InputRecord parseInputRecord(String[] row) {
        InputRecord record = new InputRecord();
        
        // 按照CSV列顺序解析
        record.clientReference = getColumnValue(row, 0);
        record.entityType = getColumnValue(row, 1);
        record.firstName = getColumnValue(row, 2);
        record.middleName = getColumnValue(row, 3);
        record.lastName = getColumnValue(row, 4);
        record.fullName = getColumnValue(row, 5);
        record.street = getColumnValue(row, 6);
        record.city = getColumnValue(row, 7);
        record.state = getColumnValue(row, 8);
        record.country = getColumnValue(row, 9);
        record.postalCode = getColumnValue(row, 10);
        record.dob = getColumnValue(row, 11);
        record.citizenship = getColumnValue(row, 12);
        record.idType = getColumnValue(row, 13);
        record.idNumber = getColumnValue(row, 14);
        
        return record;
    }
    
    /**
     * 安全获取CSV列值
     * 
     * @param row CSV行数据
     * @param index 列索引
     * @return 列值，如果不存在或为空则返回null
     */
    private static String getColumnValue(String[] row, int index) {
        if (index >= row.length) {
            return null;
        }
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }
    
    /**
     * 批量处理搜索请求
     * 
     * @param client SOAP客户端
     * @param inputRecords 输入记录列表
     * @return 搜索结果列表
     */
    private static List<SearchResult> processBatchSearch(
            BridgerSoapClient client, List<InputRecord> inputRecords) {
        
        List<SearchResult> results = new ArrayList<>();
        
        for (int i = 0; i < inputRecords.size(); i++) {
            InputRecord inputRecord = inputRecords.get(i);
            
            logger.info("处理记录 {}/{}: {}", 
                i + 1, inputRecords.size(), inputRecord.clientReference);
            
            try {
                // 构建搜索请求
                Search request = buildSearchRequest(inputRecord);
                
                // 执行搜索
                SearchResults response = client.executeSearch(request);
                System.out.println("Response -> " + XmlFormatter.toPrettyXml(response));
                // 记录成功结果
                results.add(new SearchResult(request, response));
                
                // 打印响应摘要
                if (response.getRecords() != null && 
                    response.getRecords().getResultRecord() != null) {
                    int recordCount = response.getRecords().getResultRecord().size();
                    logger.info("  -> 返回 {} 条记录", recordCount);
                } else {
                    logger.info("  -> 无匹配记录");
                }
                
                // 添加延迟，避免API限流
                if (i < inputRecords.size() - 1) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }
                
            } catch (ISearchSearchServiceFaultFaultFaultMessage e) {
                logger.error("  -> API服务错误: {}", e.getMessage());
                if (e.getFaultInfo() != null) {
                    logger.error("  -> 错误类型: {}", e.getFaultInfo().getType());
                }
                // 记录失败结果
                Search request = buildSearchRequest(inputRecord);
                results.add(new SearchResult(request, e));
                
            } catch (Exception e) {
                logger.error("  -> 处理失败: {}", e.getMessage());
                // 记录失败结果
                Search request = buildSearchRequest(inputRecord);
                results.add(new SearchResult(request, e));
            }
        }
        
        return results;
    }
    
    /**
     * 根据输入记录构建搜索请求
     * 
     * @param inputRecord 输入记录
     * @return Search请求对象
     */
    private static Search buildSearchRequest(InputRecord inputRecord) {

        SearchConfiguration sc = new SearchConfiguration();
        sc.setPredefinedSearchName("ERF Search");
        sc.setWriteResultsToDatabase(Boolean.FALSE);
        AssignmentInfo assgn = new AssignmentInfo();
        assgn.setType(AssignmentType.ROLE);
        assgn.setDivision("Default division");
        ArrayOfString as = new ArrayOfString();
        as.getString().add("MLRO / System Administrator");
        assgn.setRolesOrUsers(as);
        assgn.setEmailNotification(Boolean.FALSE);
        sc.setAssignResultTo(assgn);

        SearchRequestBuilder builder = SearchRequestBuilder.create()
            .withCredentials(CLIENT_ID, USER_ID, PASSWORD)
            .withClientReference(inputRecord.clientReference)
                .withConfig(sc);
        
        // 根据实体类型设置不同的信息
        if ("Individual".equalsIgnoreCase(inputRecord.entityType)) {
            // 个人搜索
            if (inputRecord.fullName != null) {
                builder.withFullName(
                    inputRecord.firstName,
                    inputRecord.middleName,
                    inputRecord.lastName,
                    inputRecord.fullName
                );
            } else if (inputRecord.firstName != null && inputRecord.lastName != null) {
                builder.withIndividual(inputRecord.firstName, inputRecord.lastName);
            }
        } else if ("Business".equalsIgnoreCase(inputRecord.entityType)) {
            // 企业搜索
            if (inputRecord.fullName != null) {
                builder.withBusiness(inputRecord.fullName);
            }
        }
        
        // 添加地址信息（如果有）
        if (hasAddressInfo(inputRecord)) {
            builder.withAddress(
                inputRecord.street,
                inputRecord.city,
                inputRecord.state,
                inputRecord.country,
                inputRecord.postalCode
            );
        }
        
        // 添加出生日期（如果有）
        if (inputRecord.dob != null) {
            builder.withDOB(inputRecord.dob);
        }
        
        // 添加国籍（如果有）
        if (inputRecord.citizenship != null) {
            builder.withCitizenship(inputRecord.citizenship);
        }
        
        // 添加ID信息（如果有）
        if (inputRecord.idType != null && inputRecord.idNumber != null) {
            builder.withID(inputRecord.idType, inputRecord.idNumber);
        }

        System.out.println("XML Request ->" + builder.toPrettyXml());

        return builder.build();
    }
    
    /**
     * 检查是否有地址信息
     * 
     * @param record 输入记录
     * @return 如果有任何地址字段则返回true
     */
    private static boolean hasAddressInfo(InputRecord record) {
        return record.street != null || 
               record.city != null || 
               record.state != null || 
               record.country != null || 
               record.postalCode != null;
    }
    
    /**
     * 导出搜索结果到CSV文件
     * 
     * @param results 搜索结果列表
     * @param outputPath 输出文件路径
     * @throws Exception 如果导出失败
     */
    private static void exportResults(List<SearchResult> results, Path outputPath) 
            throws Exception {
        
        // 使用CsvExporter导出所有结果
        CsvExporter.exportBatchResults(outputPath, results);
        
        logger.info("成功导出 {} 条结果到CSV文件", results.size());
    }
    
    /**
     * 生成输出文件名
     * 格式：output_YYYYMMdd_HH.MM.sss.csv
     * 
     * @return 输出文件名
     */
    private static String generateOutputFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH.mm.SSS");
        return "output/output_" + now.format(formatter) + ".csv";
    }
    
    /**
     * 统计成功的搜索数量
     * 
     * @param results 搜索结果列表
     * @return 成功数量
     */
    private static int countSuccessful(List<SearchResult> results) {
        return (int) results.stream().filter(r -> r.error == null).count();
    }
    
    /**
     * 统计失败的搜索数量
     * 
     * @param results 搜索结果列表
     * @return 失败数量
     */
    private static int countFailed(List<SearchResult> results) {
        return (int) results.stream().filter(r -> r.error != null).count();
    }
    
    /**
     * 打印统计信息
     * 
     * @param results 搜索结果列表
     */
    private static void printStatistics(List<SearchResult> results) {
        logger.info("==================== 统计信息 ====================");
        logger.info("总请求数: {}", results.size());
        logger.info("成功数: {}", countSuccessful(results));
        logger.info("失败数: {}", countFailed(results));
        
        // 统计返回的记录总数
        int totalRecords = 0;
        int totalWatchlistMatches = 0;
        
        for (SearchResult result : results) {
            if (result.error == null && result.response != null) {
                if (result.response.getRecords() != null && 
                    result.response.getRecords().getResultRecord() != null) {
                    
                    List<ResultRecord> records = result.response.getRecords().getResultRecord();
                    totalRecords += records.size();
                    
                    // 统计监控列表匹配
                    for (ResultRecord record : records) {
                        if (record.getWatchlist() != null &&
                            record.getWatchlist().getMatches() != null &&
                            record.getWatchlist().getMatches().getWLMatch() != null) {
                            
                            totalWatchlistMatches += 
                                record.getWatchlist().getMatches().getWLMatch().size();
                        }
                    }
                }
            }
        }
        
        logger.info("返回记录总数: {}", totalRecords);
        logger.info("监控列表匹配总数: {}", totalWatchlistMatches);
        
        if (totalWatchlistMatches > 0) {
            logger.warn("⚠️  发现监控列表匹配，请检查输出文件！");
        }
        
        logger.info("==================================================");
    }
}
