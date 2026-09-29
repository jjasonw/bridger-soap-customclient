package com.lexisnexis.bridger.client;

import com.lexisnexis.bridger.generated.*;
import com.lexisnexis.bridger.util.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.ws.BindingProvider;
import java.net.URL;

/**
 * Bridger SOAP客户端封装类
 * 
 * 该类封装了与LexisNexis Bridger Insight API 12.0的SOAP通信逻辑。
 * 主要功能包括：
 * 1. 初始化SOAP服务连接
 * 2. 执行搜索请求
 * 3. 处理响应数据
 * 
 * @author Bridger SOAP Client Generator
 * @version 1.0.0
 */
public class BridgerSoapClient {
    
    private static final Logger logger = LoggerFactory.getLogger(BridgerSoapClient.class);

    /**
     * SOAP服务端点URL
     */
    public static final String SERVICE_ENDPOINT =
        ApplicationConfiguration.getRequiredProperty("bridger.api.endpoint");

    public static final String SERVICE_NAMESPACE =
        ApplicationConfiguration.getRequiredProperty("bridger.api.servicenamespace");

    /**
     * ISearch服务接口实例
     * 该接口由Apache CXF从WSDL自动生成，提供Search操作
     */
    private final ISearch searchService;
    
    /**
     * 构造函数 - 使用默认的WSDL位置和服务端点
     * 
     * @throws Exception 如果服务初始化失败
     */
    public BridgerSoapClient() throws Exception {
        this(getBundledWsdlLocation(), SERVICE_ENDPOINT);
    }

    private static URL getBundledWsdlLocation() {
        URL wsdlLocation = XGServices.class.getResource("/API12.1.wsdl");
        if (wsdlLocation == null) {
            throw new IllegalStateException("API12.1.wsdl was not found on the application classpath");
        }
        return wsdlLocation;
    }

    /**
     * 构造函数 - 使用自定义WSDL位置和服务端点
     * 
     * @param wsdlLocation WSDL文件的URL位置，如果为null则使用默认位置
     * @param endpointUrl 服务端点URL
     * @throws Exception 如果服务初始化失败
     */
    public BridgerSoapClient(URL wsdlLocation, String endpointUrl) throws Exception {
        logger.info("初始化Bridger SOAP客户端...");
        
        // 创建XGServices服务实例
        XGServices service;
        if (wsdlLocation != null) {
            service = new XGServices(wsdlLocation);
        } else {
            service = new XGServices();
        }
        
        // 获取ISearch端口
        searchService = service.getBasicHttpBindingISearch();
        
        // 设置服务端点地址
        BindingProvider bindingProvider = (BindingProvider) searchService;
        bindingProvider.getRequestContext().put(
            BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
            endpointUrl
        );
        
        logger.info("Bridger SOAP客户端初始化完成，端点: {}", endpointUrl);
    }
    
    /**
     * 执行搜索操作
     * 
     * 该方法接收一个Search请求对象，调用SOAP服务的Search操作，
     * 并返回SearchResponse响应对象。
     * 
     * @param searchRequest 搜索请求对象，包含context、config和input等信息
     * @return SearchResponse 搜索响应对象，包含搜索结果记录
     * @throws ISearchSearchServiceFaultFaultFaultMessage 如果服务端返回业务错误
     */
    public SearchResults executeSearch(Search searchRequest) 
            throws ISearchSearchServiceFaultFaultFaultMessage {
        
        logger.info("执行搜索请求...");
        
        // 记录请求的基本信息
        if (searchRequest.getContext() != null) {
            logger.debug("客户端ID: {}", searchRequest.getContext().getClientID());
            logger.debug("用户ID: {}", searchRequest.getContext().getUserID());
        }
        
        // 调用SOAP服务的Search操作
        SearchResults response = searchService.search(
            searchRequest.getContext(),
            searchRequest.getConfig(),
            searchRequest.getInput()
        );
        
        // 记录响应的基本信息
        if (response != null && response.getRecords() != null) {
            logger.info("搜索完成，返回 {} 条记录", 
                response.getRecords().getResultRecord() != null ? 
                response.getRecords().getResultRecord().size() : 0);
        }
        
        return response;
    }
    
    /**
     * 获取ISearch服务接口实例
     * 
     * @return ISearch服务接口
     */
    public ISearch getSearchService() {
        return searchService;
    }
}
