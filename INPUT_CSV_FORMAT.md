# Input CSV 格式说明

## 文件名

`src/main/resources/input.csv` - 放置在项目根目录

## CSV格式

### 列定义

| 列名 | 是否必需 | 说明 | 示例 |
|------|---------|------|------|
| ClientReference | 必需 | 客户端参考号，用于标识每个请求 | SEARCH-001 |
| EntityType | 必需 | 实体类型：Individual（个人）或 Business（企业） | Individual |
| FirstName | 个人必需 | 名 | John |
| MiddleName | 可选 | 中间名 | M |
| LastName | 个人必需 | 姓 | Doe |
| FullName | 可选 | 全名（如果不提供，将自动组合） | John M Doe |
| Street | 可选 | 街道地址 | 123 Main Street |
| City | 可选 | 城市 | New York |
| State | 可选 | 州/省 | NY |
| Country | 可选 | 国家（ISO 3166-1 alpha-3代码） | USA |
| PostalCode | 可选 | 邮政编码 | 10001 |
| DOB | 可选 | 出生日期（格式：YYYY-MM-DD） | 1980-01-01 |
| Citizenship | 可选 | 国籍（ISO 3166-1 alpha-3代码） | USA |
| IDType | 可选 | ID类型（如：Account, Passport, SSN等） | Account |
| IDNumber | 可选 | ID号码 | ACC123456 |

### 注意事项

1. **编码**：CSV文件必须使用UTF-8编码
2. **分隔符**：使用逗号（,）作为字段分隔符
3. **头部行**：第一行必须是列名（如上表所示）
4. **空值**：可选字段可以留空，但必须保留逗号分隔符
5. **引号**：如果字段值包含逗号或换行符，需要用双引号包围
6. **日期格式**：DOB必须使用YYYY-MM-DD格式（如：1980-01-01）
7. **实体类型**：
   - Individual：个人搜索（需要FirstName和LastName）
   - Business：企业搜索（需要FullName作为企业名称）

## 示例

### 示例1：完整信息的个人搜索

```csv
ClientReference,EntityType,FirstName,MiddleName,LastName,FullName,Street,City,State,Country,PostalCode,DOB,Citizenship,IDType,IDNumber
SEARCH-001,Individual,John,M,Doe,John M Doe,123 Main Street,New York,NY,USA,10001,1980-01-01,USA,Account,ACC123456
```

### 示例2：最小信息的个人搜索

```csv
ClientReference,EntityType,FirstName,MiddleName,LastName,FullName,Street,City,State,Country,PostalCode,DOB,Citizenship,IDType,IDNumber
SEARCH-002,Individual,Jane,,Smith,,,Los Angeles,CA,USA,,,,,
```

### 示例3：企业搜索

```csv
ClientReference,EntityType,FirstName,MiddleName,LastName,FullName,Street,City,State,Country,PostalCode,DOB,Citizenship,IDType,IDNumber
SEARCH-003,Business,,,,"Acme Corporation",123 Business St,Los Angeles,CA,USA,90001,,,,
```

### 示例4：批量搜索

```csv
ClientReference,EntityType,FirstName,MiddleName,LastName,FullName,Street,City,State,Country,PostalCode,DOB,Citizenship,IDType,IDNumber
SEARCH-001,Individual,John,M,Doe,John M Doe,123 Main Street,New York,NY,USA,10001,1980-01-01,USA,Account,ACC123456
SEARCH-002,Individual,Jane,,Smith,Jane Smith,456 Oak Avenue,Los Angeles,CA,USA,90001,1985-05-15,USA,Account,ACC789012
SEARCH-003,Individual,Robert,J,Johnson,Robert J Johnson,789 Pine Road,Chicago,IL,USA,60601,1990-12-30,USA,Account,ACC345678
```

## 在Excel中创建input.csv

1. 打开Excel
2. 在第一行输入列名（如上表所示）
3. 从第二行开始输入数据
4. 选择 `文件` → `另存为`
5. 选择 `CSV UTF-8（逗号分隔）(*.csv)` 格式
6. 文件名输入 `src/main/resources/input.csv`
7. 保存到项目根目录

## 验证CSV格式

可以使用以下方法验证CSV格式：

### 方法1：在记事本中查看

打开input.csv，应该看到类似：
```
ClientReference,EntityType,FirstName,MiddleName,LastName,...
SEARCH-001,Individual,John,M,Doe,...
```

### 方法2：在Excel中验证

1. 打开Excel
2. 选择 `数据` → `从文本/CSV`
3. 选择input.csv文件
4. 确认分隔符为逗号
5. 确认数据正确显示

## 常见错误

### 错误1：编码问题

**症状**：中文或特殊字符显示为乱码

**解决**：确保使用UTF-8编码保存CSV文件

### 错误2：日期格式错误

**症状**：程序报错"Invalid date format"

**解决**：确保DOB使用YYYY-MM-DD格式（如：1980-01-01）

### 错误3：缺少必需字段

**症状**：程序报错"Required field missing"

**解决**：
- Individual类型必须提供FirstName和LastName
- Business类型必须提供FullName
- 所有记录必须提供ClientReference和EntityType

### 错误4：字段值包含逗号

**症状**：数据列错位

**解决**：用双引号包围包含逗号的字段值
```csv
SEARCH-001,Individual,John,M,Doe,"Apartment 5, Building A",New York,NY,USA,10001,...
```

## 处理流程

程序读取input.csv后，会：

1. 逐行读取CSV文件（跳过头部行）
2. 为每一行创建一个Search请求
3. 调用Bridger API执行搜索
4. 将所有结果写入到output_YYYYMMdd_HH.MM.sss.csv文件
5. 在请求之间添加1秒延迟，避免API限流

## 输出文件

输出文件名格式：`output_YYYYMMdd_HH.MM.sss.csv`

示例：
- `output_20260205_14.30.123.csv`
- `output_20260205_09.15.456.csv`

输出文件位于项目根目录，包含：
- 所有输入的请求信息
- 对应的响应数据
- CSV头部信息

---

**提示**：建议先使用少量数据（2-3条）测试，确认格式正确后再进行大批量处理。
