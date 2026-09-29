package com.lexisnexis.bridger.service;

import com.lexisnexis.bridger.generated.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
/**
 * 搜索请求构建器
 * 
 * 该类提供了便捷的方法来构建Bridger API的Search请求对象。
 * 使用Builder模式，支持链式调用，简化请求对象的创建过程。
 * 
 * 使用示例：
 * <pre>
 * Search request = SearchRequestBuilder.create()
 *     .withCredentials("clientId", "userId", "password")
 *     .withIndividual("John", "Doe")
 *     .withAddress("123 Main St", "New York", "NY", "USA", "10001")
 *     .withDOB("1980-01-01")
 *     .build();
 * </pre>
 * 
 * @author Bridger SOAP Client Generator
 * @version 1.0.0
 */
public class SearchRequestBuilder {
    
    private final Search search;
    private final ClientContext context;
    private final InputRecord input;
    private final SearchInput searchInput;
    
    /**
     * 私有构造函数
     * 使用create()静态方法创建实例
     */
    private SearchRequestBuilder() {
        this.search = new Search();
        this.context = new ClientContext();
        this.searchInput = new SearchInput();
        this.input = new InputRecord();
        
        // 设置关系
        search.setContext(context);
        search.setInput(searchInput);
        
        // 创建记录列表
        ArrayOfInputRecord records = new ArrayOfInputRecord();
        records.getInputRecord().add(input);
        searchInput.setRecords(records);
        
        // 初始化Entity
        InputEntity entity = new InputEntity();
        input.setEntity(entity);
    }
    
    /**
     * 创建SearchRequestBuilder实例
     * 
     * @return SearchRequestBuilder实例
     */
    public static SearchRequestBuilder create() {
        return new SearchRequestBuilder();
    }
    
    /**
     * 设置认证凭据
     * 
     * @param clientID 客户端ID（必需）
     * @param userID 用户ID（必需）
     * @param password 密码（必需）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withCredentials(String clientID, String userID, String password) {
        context.setClientID(clientID);
        context.setUserID(userID);
        context.setPassword(password);
        return this;
    }
    
    /**
     * 设置客户端参考号
     * 
     * @param clientReference 客户端参考号（可选）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withClientReference(String clientReference) {
        context.setClientReference(clientReference);
        return this;
    }
    
    /**
     * 设置DPPA选项
     * 
     * @param dppa DPPA选项（驾驶员隐私保护法）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withDPPA(DPPAChoiceType dppa) {
        context.setDPPA(dppa);
        return this;
    }
    
    /**
     * 设置GLB标识
     * 
     * @param glb GLB标识（Gramm-Leach-Bliley Act）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withGLB(Integer glb) {
        context.setGLB(glb);
        return this;
    }
    
    /**
     * 设置个人实体信息
     * 
     * @param firstName 名（可选）
     * @param lastName 姓（可选）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withIndividual(String firstName, String lastName) {
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        entity.setEntityType(InputEntityType.INDIVIDUAL);
        
        InputName name = new InputName();
        name.setFirst(firstName);
        name.setLast(lastName);
        entity.setName(name);
        
        return this;
    }
    
    /**
     * 设置完整姓名信息
     * 
     * @param firstName 名（可选）
     * @param middleName 中间名（可选）
     * @param lastName 姓（可选）
     * @param fullName 全名（可选）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withFullName(String firstName, String middleName, 
                                              String lastName, String fullName) {
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        entity.setEntityType(InputEntityType.INDIVIDUAL);
        
        InputName name = new InputName();
        name.setFirst(firstName);
        name.setMiddle(middleName);
        name.setLast(lastName);
        name.setFull(fullName);
        entity.setName(name);
        
        return this;
    }
    
    /**
     * 设置企业实体信息
     * 
     * @param businessName 企业名称
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withBusiness(String businessName) {
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        entity.setEntityType(InputEntityType.BUSINESS);
        
        InputName name = new InputName();
        name.setFull(businessName);
        entity.setName(name);
        
        return this;
    }
    
    /**
     * 添加地址信息
     * 
     * @param street 街道地址（可选）
     * @param city 城市（可选）
     * @param state 州/省（可选）
     * @param country 国家（可选）
     * @param postalCode 邮政编码（可选）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withAddress(String street, String city, String state, 
                                             String country, String postalCode) {
        InputAddress address = new InputAddress();
        address.setStreet1(street);
        address.setCity(city);
        address.setStateProvinceDistrict(state);
        address.setCountry(country);
        address.setPostalCode(postalCode);
        // 设置AddressType
        try {
            address.setType(AddressType.fromValue("Current"));
        } catch (IllegalArgumentException e) {
            // 如果不是标准枚举值，使用NONE
            address.setType(AddressType.NONE);
        }
        
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        if (entity.getAddresses() == null) {
            entity.setAddresses(new ArrayOfInputAddress());
        }
        entity.getAddresses().getInputAddress().add(address);
        
        return this;
    }
    
    /**
     * 添加出生日期信息
     * 
     * @param dob 出生日期，格式：YYYY-MM-DD（可选）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withDOB(String dob) {
        InputAdditionalInfo dobInfo = new InputAdditionalInfo();
        dobInfo.setType(AdditionalInfoType.DOB);
        dobInfo.setValue(dob);
        
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        if (entity.getAdditionalInfo() == null) {
            entity.setAdditionalInfo(new ArrayOfInputAdditionalInfo());
        }
        entity.getAdditionalInfo().getInputAdditionalInfo().add(dobInfo);
        
        return this;
    }
    
    /**
     * 添加国籍信息
     * 
     * @param citizenship 国籍，使用ISO 3166-1 alpha-3代码（如：USA, GBR）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withCitizenship(String citizenship) {
        InputAdditionalInfo citizenshipInfo = new InputAdditionalInfo();
        citizenshipInfo.setType(AdditionalInfoType.CITIZENSHIP);
        citizenshipInfo.setValue(citizenship);
        
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        if (entity.getAdditionalInfo() == null) {
            entity.setAdditionalInfo(new ArrayOfInputAdditionalInfo());
        }
        entity.getAdditionalInfo().getInputAdditionalInfo().add(citizenshipInfo);
        
        return this;
    }
    
    /**
     * 添加ID信息
     * 
     * @param type ID类型（如：Account, LexID, Passport等）
     * @param number ID号码
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withID(String type, String number) {
        InputID id = new InputID();
        // 将字符串转换为IDType枚举
        try {
            IDType idType = IDType.fromValue(type);
            id.setType(idType);
        } catch (IllegalArgumentException e) {
            // 如果不是标准枚举值，使用OTHER
            id.setType(IDType.OTHER);
        }
        id.setNumber(number);
        
        InputEntity entity = input.getEntity();
        if (entity == null) {
            entity = new InputEntity();
            input.setEntity(entity);
        }
        
        if (entity.getIDs() == null) {
            entity.setIDs(new ArrayOfInputID());
        }
        entity.getIDs().getInputID().add(id);
        
        return this;
    }
    
    /**
     * 设置配置选项
     * 
     * @param config 配置对象（可选）
     * @return SearchRequestBuilder实例，支持链式调用
     */
    public SearchRequestBuilder withConfig(SearchConfiguration config) {
        search.setConfig(config);
        return this;
    }
    
    /**
     * 构建Search请求对象
     * 
     * @return Search请求对象
     */
    public Search build() {
        return search;
    }
    
    /**
     * 获取Context对象（用于高级定制）
     * 
     * @return Context对象
     */
    public ClientContext getContext() {
        return context;
    }
    
    /**
     * 获取InputRecord对象（用于高级定制）
     * 
     * @return InputRecord对象
     */
    public InputRecord getInput() {
        return input;
    }



    /**
     * 将当前构建的 Search 对象格式化为 XML 字符串
     * 用于调试打印
     */
    public String toPrettyXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Search.class);
            Marshaller marshaller = context.createMarshaller();

            // 格式化输出
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(this.search, writer);

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to marshal Search to XML", e);
        }
    }

}
