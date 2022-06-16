使用方法，只需要往gateway项目添加依赖即可：
<dependency>
    <groupId>com.mawujun</groupId>
    <artifactId>leon-cloud-gateway-plus</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>

这是ribbon的负载均衡器的版本，是老的

主要用于智能路由，所有开发人员不需要所有微服务都打开，只第需要打开自己的微服务，
一些公共微服务比如网关，认证，其他依赖微服务，会自动路由到测试机上的微服务。

在调试的时候，会自动路由到本机的微服务上，即使其他机器或测试机上同个微服务也开启了。

如果前端开发人员，用带ip地址(自己机器的IP地址)的访问本地的前端项目，比如http://xxx.xxx.xxx.xxx:80/，不要使用
http://localhost:80/ 或 http://127.0.0.1:80/
前端人员配置的时候，把后台地址配置成(测试机)：10.22.1.43.
否则会出现取不到数据。


后端开发者，可以直接通过测试库地址：http://10.22.1.43:81/
进行测试，只要自己本地的"对应服务"打开即可。
也可以访问对应的“”前端人员的ip地址“”进行测试，只要后端人员自己在自己机器上打开对应的后端服务即可。