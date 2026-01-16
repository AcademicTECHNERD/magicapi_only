此为magicapi独立可运行项目，通过run模块即可启动本地magicapi源代码。
该项目是为将定时任务逻辑迁移到xxl-job而生，其中存在非常多的逻辑改动，不适合独立运行，需要先启动xxl-job-admin，并且需要修改yml指定调度中心（xxl-job-admin），而后启动，并且xxl-job也不是源代码而是二开的才行。

# 特性
- 支持MySQL、MariaDB、Oracle、DB2、PostgreSQL、SQLServer 等支持jdbc规范的数据库
- 支持非关系型数据库Redis、Mongodb
- 支持集群部署、接口自动同步。
- 支持分页查询以及自定义分页查询
- 支持多数据源配置，支持在线配置数据源
- 支持SQL缓存，以及自定义SQL缓存
- 支持自定义JSON结果、自定义分页结果
- 支持对接口权限配置、拦截器等功能
- 支持运行时动态修改数据源
- 支持Swagger接口文档生成
- 基于[magic-script](https://gitee.com/ssssssss-team/magic-script)脚本引擎，动态编译，无需重启，实时发布
- 支持Linq式查询，关联、转换更简单
- 支持数据库事务、SQL支持拼接，占位符，判断等语法
- 支持文件上传、下载、输出图片
- 支持脚本历史版本对比与恢复
- 支持脚本代码自动提示、参数提示、悬浮提示、错误提示
- 支持导入Spring中的Bean、Java中的类
- 支持在线调试
- 支持自定义工具类、自定义模块包、自定义类型扩展、自定义方言、自定义列名转换等自定义操作

# 快速开始


