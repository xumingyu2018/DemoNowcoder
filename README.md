# 仿牛客网项目学习

# 主页讨论区分页查询功能！

## 1.首先设计Dao层接口（实体类略）

**以下是查询功能不包括分页** **（其中userId在DiscussPost类中作为外键）**

```java
//查询
//userId=0为所有帖子，1为我的帖子
//每个参数必须加@Param("")
List<DiscussPost> selectDiscussPosts(@Param("userId") int userId,@Param("offset")int offset,@Param("limit")int limit);

//为分页查询服务的查询总条数
//给参数起别名，如果只有一个参数并且要在<if>里使用，则必须加别名
int selectDiscussRows(@Param("userId")int userId);
```

```sql
  <!------------- Mapper.xml ------------->
  <sql id="selectFields">
      id,user_id,title,content,type,status,create_time,comment_count,score
  </sql>
  <sql id="insertFields">
      user_id,title,content,type,status,create_time,comment_count,score
  </sql>

  <!--查询不是被拉黑的帖子并且userId不为0按照type指定，时间排序-->
  <select id="selectDiscussPosts" resultType="DiscussPost">
      select <include refid="selectFields"></include>
      from discuss_post
      where status!=2
      <if test="userId!=0">
          and user_id=#{userId}
      </if>
      <if test="orderMode==0">
          order by type desc,create_time desc
      </if>
      <if test="orderMode==1">
          order by type desc,score desc,create_time desc
      </if>
      limit #{offset},#{limit}
  </select>

  <!--userId=0查所有;userId!=0查个人发帖数-->
  <select id="selectDiscussRows" resultType="int">
      select count(id)
      from discuss_post
      where status!=2
      <if test="userId!=0">
          and user_id=#{userId}
      </if>
  </select>

```

## 2.然后设计Service层调用Dao层接口

```java
  @Autowired
  private DiscussPostMapper discussPostMapper;
  
  public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit){
      return discussPostMapper.selectDiscussPosts(userId,offset,limit);
  }
  
  public int findDiscussPostRows(int userId){
      return discussPostMapper.selectDiscussRows(userId);
  }
```

## 3.其次封装分页功能

**封装分页功能相关信息在Page类！！**

```java
public class Page {

    //当前页面
    private int current=1;
    //显示上限
    private int limit=6;
    //数据总数(用于计算总页数)
    private int rows;
    //查询路径(用于复用分页链接)
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        //要作输入判断
        if (current>=1){
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit>=1&&limit<=100){
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows>=0){
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    /** 获取当前页的起始行**/
    public int getOffset(){
        //current*limit-limit
        return (current-1)*limit;
    }
    
    /**获取总页数**/
    public int getTotal(){
        //rows/limit[+1]
        if (rows%limit==0){
            return rows/limit;
        }else{
            return rows/limit+1;
        }
    }
    
    /**获取起始页码**/
    public int getFrom(){
        int from=current-2;
        return from < 1 ? 1 : from;
    }
    
    /**获取结束页码**/
    public int getTo(){
        int to=current+2;
        int total=getTotal();
        return to > total ? total : to;
    }
}
```

## 4.最后设计Controller层

```java
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/index",method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page){//传入model参数是因为要返回值给View
        /*方法调用前，springMVC自动实例化Model和Page,并将Page注入Model
          在thymeleaf中可以直接访问Page对象中的数据 */
        
        //分页
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/community/index");
        
        //查询所有，起始为page.getOffset()，终止为page.getLimit()个帖子，
        List<DiscussPost> list=discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        
        /*将查询的post帖子和user用户名拼接后放入map中,最后把全部map放入新的List中,
          因为UserId是外键，需要显示的是对应的名字即可 */
        List<Map<String,Object>> discussPost =new ArrayList<>();

        if (list!=null){
            for(DiscussPost post:list){
                HashMap<String, Object> map = new HashMap<>();
                // 将查询到的帖子放入map
                map.put("post",post);
                // 将发布帖子对应的用户id作为参数
                User user = userService.findUser(post.getUserId());
                // 将发帖子的所有用户放入map
                map.put("user",user);
                // 显示帖子点赞数量
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                
                //将组合的map放入List<>
                discussPost.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPost);
        return "/index";
    }
```

## 5.前端页面设计（Thymeleaf）

### 5.1查询页面

```html
  <!-- 帖子列表 -->
  <ul class="list-unstyled">
  <!--th:each="map:${discussPosts}循环遍历model.addAttribute传过来的discussPosts这个集合，每次循环得到map对象-->
    <li class="media pb-3 pt-3 mb-3 border-bottom" th:each="map:${discussPosts}">
      <a href="site/profile.html">
  <!--th:src="${map.user.headerUrl}"底层是map.get("user")->user.get("headerUrl")-->
        <img th:src="${map.user.headerUrl}" class="mr-4 rounded-circle" alt="用户头像" style="width:50px;height:50px;">
      </a>
      <div class="media-body">
        <h6 class="mt-0 mb-3">
  <!--th:utext可以转义文本中特殊字符-->
          <a href="#" th:utext="${map.post.title}">备战春招，面试刷题跟他复习，一个月全搞定！</a>
          <span class="badge badge-secondary bg-primary" th:if="${map.post.type==1}">置顶</span>
          <span class="badge badge-secondary bg-danger" th:if="${map.post.status==1}">精华</span>
        </h6>
        <div class="text-muted font-size-12">
  <!--th:text="${#dates.format(map.post.createTime)} #是引用thymeleaf自带的工具-->
          <u class="mr-3" th:utext="${map.user.username}">寒江雪</u> 发布于 <b th:text="${#dates.format(map.post.createTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-15 15:32:18</b>
          <ul class="d-inline float-right">
            <li class="d-inline ml-2">赞 11</li>
            <li class="d-inline ml-2">|</li>
            <li class="d-inline ml-2">回帖 7</li>
          </ul>
        </div>
      </div>            
    </li>
  </ul>
```

### 5.2分页功能页面

```html
  <!-- 分页 -->
  <nav class="mt-5" th:if="${page.rows>0}" th:fragment="pagination">
    <ul class="pagination justify-content-center">
      <li class="page-item">
  <!--th:href="@{${page.path}(current=1,limit=5)}"等效于/index?current=1&limit=5-->
        <a class="page-link" th:href="@{${page.path}(current=1)}">首页</a>
      </li>
  <!--th:class="|page-item ${page.current==1?'disabled':''}|" 动态上一页禁用  固定数据+变量使用方法:加|| -->
      <li th:class="|page-item ${page.current==1?'disabled':''}|">
        <a class="page-link" th:href="@{${page.path}(current=${page.current-1})}">上一页</a>
      </li>
  <!--#numbers.sequence #调用thymelead自带工具numbers,从from到to的数组-->
      <li th:class="|page-item ${i==page.current?'active':''}|" th:each="i:${#numbers.sequence(page.from,page.to)}">
        <a class="page-link" href="#" th:text="${i}">1</a>
      </li>

      <li th:class="|page-item ${page.current==page.total?'disabled':''}|">
        <a class="page-link" th:href="@{${page.path}(current=${page.current+1})}">下一页</a>
      </li>
      <li class="page-item">
        <a class="page-link" th:href="@{${page.path}(current=${page.total})}">末页</a>
      </li>
    </ul>
  </nav>
```

# 注册登录功能

## 发送邮件

### 1.邮箱设置：启用SMTP服务

### 2.SpringEmail

#### 2.1配置xml文件

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
    <version>2.6.6</version>
</dependency>

```

#### 2.2在application.yml配置邮箱参数

```yaml
#  配置邮箱
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: xxx@qq.com //本网站的发送方
    password: xxx  //密码为生成授权码后给的密码
    protocol: smtps
```

#### 2.3创建MailClient邮箱工具类

```java
@Component
public class MailClient {

    private static final Logger logger= LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")//将yml的属性注入到from
    private String from;

    public void sendMail(String to,String subject,String content){
        try {
        //MimeMessage用于封装邮件相关信息
            MimeMessage message = javaMailSender.createMimeMessage();
            //需要一个邮件帮助器，负责构建MimeMessage对象
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            //支持HTML文本
            helper.setText(content,true);
            //发送邮件都有JavaMailSender来做
            javaMailSender.send(helper.getMimeMessage());
        }catch (MessagingException e){
            logger.error("发送邮件失败："+e.getMessage());
        }
    }
}
```

#### 2.4测试类

````java
@Autowired
private MailClient mailClient;

@Autowired
private TemplateEngine templateEngine;//注入HTML模板引擎类，模板格式化

@Test
public void testTextMail(){//发送文本类型邮件
    mailClient.sendMail("xmy981022@163.com","Test","Welcome");
}

@Test
public void testHTMLMail(){//发送thymeleaf html类型文件
    Context context = new Context();
    context.setVariable("username","Nevermore");
    String content = templateEngine.process("/mail/activation", context);
    mailClient.sendMail("xmy981022@163.com","HTML",content);
}


注意：JavaMailSender和TemplateEngine会被自动注入到spring中

````

## 注册功能


### 1.配置application.properties文件


````yml
community.path.domain: http://localhost:8080
server.servlet.context-path: /community
````

### 2.创建工具类（处理MD5加密、生成随机数、激活标志接口）

```java
public class CommunityUtil {
    /*
    * 生成随机字符串
    * 用于邮件激活码，salt5位随机数加密
    **/
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }
    /* MD5加密
    * hello-->abc123def456
    * hello + 3e4a8-->abc123def456abc
    */
    public static String md5(String key){
        if (StringUtils.isBlank(key)){
            return null;
        }
        //MD5加密方法
        return DigestUtils.md5DigestAsHex(key.getBytes());
        //参数是bytes型
    }
}
```

```java
public interface CommuityConstant {
    /*      以下用于注册功能      */
    /** 激活成功*/
    int ACTIVATION_SUCCESS=0;
    /** 重复激活 */
    int ACTIVATION_REPEAT=1;
    /** 激活失败 */
    int ACTIVATION_FAILURE=2;
    
    /*      以下用于登录功能*      /
    /**  
     * 默认状态的登录凭证的超时时间
     */
    int DEFAULT_EXPIRED_SECONDS=3600*12;
    /**
     * 记住状态的登录凭证超时时间
     */
    int REMEMBER_EXPIRED_SECONDS=3600*24*7;
}
```

### 3.编写Service业务层(实现CommuityConstant接口)

#### 3.1注册业务

```java
//..注入userMapper，mailClient，templateEngine
@Value("${community.path.domain}")
private String domain;
@Value("${server.servlet.context-path}")
private String contextPath;
//注册功能
/**为什么返回的是Map类型，因为用Map来存各种情况下的信息，返回给前端页面* */
public Map<String,Object> register(User user){
    HashMap<String, Object> map = new HashMap<>();
    /*
        判输入
     */
    if (user == null) {
        throw new IllegalArgumentException("参数不能为空！");
    }
    if (StringUtils.isBlank(user.getUsername())){
        map.put("usernameMsg","账户不能为空");
    }
    if (StringUtils.isBlank(user.getPassword())){
        map.put("passwordMsg","密码不能为空");
    }
    if (StringUtils.isBlank(user.getEmail())){
        map.put("emailMsg","邮箱不能为空");
    }
    /*
        判存在
     */
    User u = userMapper.selectByName(user.getUsername());
    if (u != null) {
        map.put("usernameMsg","该账号已存在！");
        return map;
    }
    u = userMapper.selectByEmail(user.getEmail());
    if (u != null) {
        map.put("emailMsg","该邮箱已被注册！");
        return map;
    }
    /*
        注册账户
        1.设置salt加密(随机5位数加入密码)
        2.设置密码+salt
        3.设置随机数激活码
        4.设置status,type=0,时间
        5.设置头像(动态)
          user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000))
     */
    user.setSalt(CommunityUtil.generateUUID().substring(0,5));
    user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
    user.setActivationCode(CommunityUtil.generateUUID());
    user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
    user.setStatus(0);
    user.setType(0);
    user.setCreateTime(new Date());
    userMapper.insertUser(user);
    /*
        激活邮件
        1.创建Context对象-->context.setVariable(name,value)将name传入前端
          为thymeleaf提供变量
        2.设置email和url
        3.templateEngine.process执行相应HTML
        4.发送邮件
     */
    Context context = new Context();
    context.setVariable("email",user.getEmail());
    //http://localhost:8080/community/activation/101/code激活链接
    String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
    context.setVariable("url",url);

    String content = templateEngine.process("/mail/activation", context);
    mailClient.sendMail(user.getEmail(),"激活账号",content);
    return map;
}
```

#### &#x20;  3.2激活邮件业务

```java
/**激活邮件功能* */
  public int activation(int userId,String code){
      User user = userMapper.selectById(userId);
      if (user.getStatus()==1){
          return ACTIVATION_REPEAT;
      }else if (user.getActivationCode().equals(code)){
          userMapper.updateStatus(userId,1);
          return ACTIVATION_SUCCESS;
      }else {
          return ACTIVATION_FAILURE;
      }
  }
```

### 4.编写Controller层

```java
//注册Controller
@RequestMapping(value = "/register",method = RequestMethod.POST)
public String register(Model model, User user){
    Map<String, Object> map = userService.register(user);
    if (map == null || map.isEmpty()){
        map.put("msg","注册成功,我们已经向您的邮件发送了一封激活邮件,请尽快激活！");
        map.put("target","/index");
        return "/site/operate-result";
    }else{
        model.addAttribute("usernameMsg",map.get("usernameMsg"));
        model.addAttribute("passwordMsg",map.get("passwordMsg"));
        model.addAttribute("emailMsg",map.get("emailMsg"));
        return "/site/register";
    }
}
```

```java
/**激活邮件Controller**/
//http://localhost:8080/community/activation/101/code激活链接
@RequestMapping(value = "/activation/{userId}/{code}",method = RequestMethod.GET)
public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
    int result = userService.activation(userId, code);
    if (result == ACTIVATION_SUCCESS){
        model.addAttribute("msg","激活成功,你的账号已经可以正常使用了！");
        model.addAttribute("target","/login");
    }else if (result == ACTIVATION_REPEAT){
        model.addAttribute("msg","无效操作,该账号已经激活过了！");
        model.addAttribute("target","/index");
    }else {
        model.addAttribute("msg","激活失败,你提供的激活码不正确！");
        model.addAttribute("target","/index");
    }
    return "/site/operate-result";
}
```

### 5.编写前端Thymeleaf页面核心点

```html
/**注册页面 */
<form class="mt-5" method="post" th:action="@{/register}">
  <div class="col-sm-10">
    <input type="text"
         th:class="|form-control ${usernameMsg!=null?'is-invalid':''}|"
         th:value="${user!=null?user.username:''}"
         id="username" name="username" placeholder="请输入您的账号!" required>
    <div class="invalid-feedback" th:text="${usernameMsg}">
      该账号已存在!    <!--该div的显示与is-invalid有关-->
    </div>
    <button type="submit" class="btn btn-info text-white form-control">立即注册</button>
  </div>
</form>

/**账号激活中间页* */
<div class="jumbotron">
  <p class="lead" th:text="${msg}">激活状态信息</p>
  <p>
    系统会在 <span id="seconds" class="text-danger">8</span> 秒后自动跳转,
    您也可以点此 <a id="target" th:href="@{${target}}" class="text-primary">链接</a>, 手动跳转!
  </p>
</div>
<!--自动跳转Js -->
<script>
  $(function(){
    setInterval(function(){
      var seconds = $("#seconds").text();
      $("#seconds").text(--seconds);
      if(seconds == 0) {
        location.href = $("#target").attr("href");
      }
    }, 1000);
  });
</script>

/**邮箱模板页* */
<div>
  <p><b th:text="${email}">xxx@xxx.com</b>, 您好!</p>
  <p>
    您正在注册xxx, 这是一封激活邮件, 请点击 
    <a th:href="${url}">此链接</a>,
    激活您的xxx账号!
  </p>
</div>
```

## 生成验证码

参考网站 ：[http://code.google.com/archive/p/kaptcha/](http://code.google.com/archive/p/kaptcha/ "http://code.google.com/archive/p/kaptcha/")

注意：1.Producer是Kaptcha的核心接口   2.DefaultKaptcha是Kaptcha核心接口的默认实现类

&#x20;     3.Spring Boot没有为Kaptcha提供自动配置

### 1.引入pom.xml

```xml
<dependency>
    <groupId>com.github.penggle</groupId>
    <artifactId>kaptcha</artifactId>
    <version>2.3.2</version>
</dependency>

```

### 2.创建配置类装配第三方bean

```java
@Configuration
public class KaptchaConfig {
    @Bean
    public Producer KaptchaProducer(){
        /**         
         * 手动创建properties.xml配置文件对象*         
         * 设置验证码图片的样式，大小，高度，边框，字体等
         */
        Properties properties=new Properties();
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "105,179,90");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.image.width", "110");
        properties.setProperty("kaptcha.image.height", "40");
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");

        DefaultKaptcha Kaptcha=new DefaultKaptcha();
        Config config=new Config(properties);
        Kaptcha.setConfig(config);

        return Kaptcha;
    }
}
```

### 3.编写Controller接口

```java
@RequestMapping(value = "/kaptcha",method = RequestMethod.GET)
public void getKaptcha(HttpServletResponse response, HttpSession session){
    //生成验证码
    String text = kaptchaProducer.createText();
    BufferedImage image = kaptchaProducer.createImage(text);
    //将验证码存入session
    session.setAttribute("kaptcha",text);
    //将图片输出给浏览器
    response.setContentType("image/png");
    try {
        ServletOutputStream os = response.getOutputStream();
        ImageIO.write(image,"png",os);
    }catch (IOException e){
        logger.error("响应验证码失败:"+e.getMessage());
    }
}
```

### 4.Thymeleaf前端页面核心点

```html
<div class="col-sm-4">
  <img th:src="@{/kaptcha}" id="kaptchaImage" style="width:100px;height:40px;" class="mr-2"/>
  <a href="javascript:refresh_kaptcha();" class="font-size-12 align-bottom">刷新验证码</a>
</div>
<!--对应的Js-->
<script>
  function refresh_kaptcha() {
    var path = CONTEXT_PATH + "/kaptcha?p=" + Math.random();
    $("#kaptchaImage").attr("src", path);
  }
</script>
<!--全局Js配置文件-->
var CONTEXT_PATH="/community";
```

## 登录功能

验证账号,密码,验证码（成功：生成登录凭证ticket，发放给客户端  失败：跳转回登录页 ）

### 1.创建登录凭证实体类（登录凭证相当于Session的作用）

注意 :**为什么要搞一个登录凭证，因为最好不要将User信息存入Model返回给前端，敏感信息尽量不要返回给浏览器，不安全，而是选择ticket凭证，通过ticket可以在服务器端得到User**

### 2.编写Dao层接口(注解方式实现)

```java
  @Insert({
          "insert into login_ticket(user_id,ticket,status,expired) ",
          "values (#{userId},#{ticket},#{status},#{expired})"
  })
  @Options(useGeneratedKeys = true,keyProperty = "id")
  //登录功能需要添加登录凭证ticket
  int insertLoginTicket(LoginTicket loginTicket);
  
  @Select({
          "select id,user_id,ticket,status,expired ",
          "from login_ticket ",
          "where ticket=#{ticket}"
  })
  //检查登录状态
  LoginTicket selectByTicket(String ticket);
  
  /**
   *  一定要加@Param()不然会报错
   *  退出功能需要修改status状态
   *  @return error:com.mysql.jdbc.MysqlDataTruncation:Data truncation:Truncated incorrect DOUBLE value:...
   */
  @Update({
          "update login_ticket set status=#{status} where ticket=#{ticket} "
  })
  int updateStatus(@Param("ticket") String ticket, @Param("status") int status);
```

### 3.编写Service层登录业务

```java
  /**登录功能**/
  public Map<String,Object> login(String username,String password,int expiredSeconds){
      HashMap<String, Object> map = new HashMap<>();
      //空值处理
      if(StringUtils.isBlank(username)){
          map.put("usernameMsg","号码不能为空！");
          return map;
      }
      if(StringUtils.isBlank(password)){
          map.put("passwordMsg","密码不能为空！");
          return map;
      }
      //验证账号
      User user = userMapper.selectByName(username);
      if (user==null){
          map.put("usernameMsg","该账号不存在！");
          return map;
      }
      //验证激活状态
      if (user.getStatus()==0){
          map.put("usernameMsg","该账号未激活！");
          return map;
      }
      //验证密码(先加密再对比)
      password=CommunityUtil.md5(password+user.getSalt());
      if (!user.getPassword().equals(password)){
          map.put("passwordMsg","密码输入错误！");
          return map;
      }
      //生成登录凭证(相当于记住我这个功能==session)
      LoginTicket ticket = new LoginTicket();
      ticket.setUserId(user.getId());
      ticket.setTicket(CommunityUtil.generateUUID());
      ticket.setStatus(0);
      //当前时间的毫秒数+过期时间毫秒数
      ticket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
      loginTicketMapper.insertLoginTicket(ticket);

      map.put("ticket",ticket.getTicket());

      return map;
  }
```

### 4.编写Controller层

```java
   /**
    * 登录功能
    * @param username
    * @param password
    * @param code 用于校验验证码
    * @param rememberme  记住我（登录凭证）
    * @param model 用于将数据传递给前端页面
    * @param session 用于获取kaptcha验证码
    * @param response 用于浏览器接受cookie
    * @return
    */
    @RequestMapping(value = "/login",method = RequestMethod.POST)
    /**注意username,password这些没有封装进model* */
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session,HttpServletResponse response){
        //首先检验验证码
        String kaptcha = (String) session.getAttribute("kaptcha");
        if (StringUtils.isBlank(kaptcha)||StringUtils.isBlank(code)||!kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","验证码不正确！");
            return "/site/login";
        }
        /**
         * 1.验证用户名和密码(重点)
         * 2.传入浏览器cookie=ticket
         */
        int expiredSeconds=rememberme?REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }
```

### 5.编写前端Thymeleaf页面核心点

```html
<div class="col-sm-10">
  <input type="text"
       th:class="|form-control ${usernameMsg!=null?'is-invalid':''}|"
       <!--注意param,因为username,password这些没有封装进model-->
       th:value="${param.username}"
       id="username" name="username" placeholder="请输入您的账号!" required>
  <div class="invalid-feedback" th:text="${usernameMsg}">
    该账号不存在!
  </div>
</div>
<div class="col-sm-10">
  <input type="checkbox" id="remember-me" name="rememberme"
       th:checked="${param.rememberme}">
  <label class="form-check-label" for="remember-me">记住我</label>
  <a href="forget.html" class="text-danger float-right">忘记密码?</a>
</div>
```

## 退出登录功能

将登录凭证loginTicket中的status置为无效

### 1.编写Service层

```java
public void logout(String ticket){
    loginTicketMapper.updateStatus(ticket,1);//来源于LoginTicket的Dao层
}
```

### 2.编写Controller层

```java
  /**
   * 退出登录功能
   * @CookieValue()注解:将浏览器中的Cookie值传给参数 
   */
  @RequestMapping(value = "/logout",method = RequestMethod.GET)
  public String logout(@CookieValue("ticket") String ticket){
      userService.logout(ticket);
      return "redirect:/login";//重定向
  }
```

## 显示登录信息

涉及到 ：****拦截器，多线程****

![](image/1_b7J4nGtYHK.PNG)

### 拦截器Demo示例

注意：

       1. 拦截器需实现HandlerInterceptor接口而配置类需实现WebMvcConfigurer接口。

       2. preHandle方法在Controller之前执行，若返回false，则终止执行后续的请求。

       3. postHandle方法在Controller之后、模板页面之前执行。

       4. afterCompletion方法在模板之后执行。

       5. 通过addInterceptors方法对拦截器进行配置


**1.创建拦截器类，实现****HandlerInterceptor****接口**

```java
@Component
public class DemoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("preHandle:在Controller之前执行");
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("afterCompletion:在模板之后执行");
    }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle:在Controller之后，前端模板引擎页面渲染之前执行");
    }
}
```

**2.创建拦截器配置类，实现****WebMvcConfigurer****接口**

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private DemoInterceptor demoInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoInterceptor)
                .excludePathPatterns("/ **/ *.css","/* */*.js","/**/ *.png","/* */*.jpg","/ **/ *.jpeg")
                .addPathPatterns("/register","/login");
}
```

### 1.首先创建两个工具类降低耦合（Request获取Cookie工具类，获取凭证ticket多线程工具类）

注意：1.ThreadLocal采用**线程隔离**的方式存放数据，可以避免多线程之间出现数据访问冲突。

2.ThreadLocal提供**set**方法，能够以当前线程为key存放数据。**get**方法，能够以当前线程为key获取数据。

3.ThreadLocal提供**remove**方法，能够以当前线程为key删除数据。

```java
public class CookieUtil {
    public static String getValue(HttpServletRequest request,String name){
        if (request==null||name==null){
            throw new IllegalArgumentException("参数为空！");
        }
        Cookie[] cookies = request.getCookies();
        if (cookies!=null){
            for (Cookie cookie : cookies){
                if (cookie.getName().equals(name)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

```java
@Component  //放入容器里不用设为静态方法
public class HostHolder {
//key就是线程对象，值为线程的变量副本
    private ThreadLocal<User> users = new ThreadLocal<>();
    /**以线程为key存入User* */
    public void setUser(User user){
        users.set(user);
    }
    /**从ThreadLocal线程中取出User* */
    public User getUser(){
        return users.get();
    }
    /**释放线程* */
    public void clear(){
        users.remove();
    }
}
```

### 2.编写Service层

```java
/**通过Cookie=ticket获取登录用户* */
public LoginTicket getLoginTicket(String ticket){
    return loginTicketMapper.selectByTicket(ticket);
}
```

### 3.创建登录凭证拦截器类（等同于Controller类）

```java
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;

    @Override
    /**在Controller访问所有路径之前获取凭证* */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**从浏览器Cookie中获取凭证* */
        String ticket=CookieUtil.getValue(request,"ticket");

        if (ticket!=null){
            //查询凭证
            LoginTicket loginTicket = userService.getLoginTicket(ticket);
            //检查凭证是否有效(after：当前时间之后)
            if (loginTicket!=null&&loginTicket.getStatus()==0&&loginTicket.getExpired().after(new Date())){
                //根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                /**在本次请求中持有用户
                 * 类似于存入Map,只是考虑到多线程
                 */
                hostHolder.setUser(user);
            }
        }
        return true;
    }
    @Override
    /**模板之前处理数据* */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user!=null && modelAndView !=null){
            modelAndView.addObject("loginUser",user);
        }
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //释放线程资源
        hostHolder.clear();
    }
}
```

### 4.编写拦截器配置类

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/* */*.css","/**/ *.js","/* */*.png","/ **/ *.jpg","/* */*.jpeg");
    }}
```

### 5.前端页面核心点修改

th:if="\${loginUser!=null}" **存在凭证显示\<li>,不存在则不显示**

```html
<li class="nav-item ml-3 btn-group-vertical" th:if="${loginUser!=null}">
  <a class="nav-link position-relative" href="site/letter.html">消息<span class="badge badge-danger">12</span></a>
</li>
```

## 拦截未登录页面的路径访问(自定义拦截器注解)

常用的元注解： **@Target：注解作用目标（方法or类）   @Retention：注解作用时间（运行时or编译时） @Document：注解是否可以生成到文档里  @Inherited**：**注解继承该类的子类将自动使用@Inherited修饰**

 注意： **若有2个拦截器，拦截器执行顺序为注册在WebMvcConfig配置类中的顺序**

### 1.自定义拦截方法类注解(annotation包)并加在需要拦截的方法上

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标记未登录时要拦截的路径访问方法
 */
public @interface LoginRequired {
}
/**加在需要拦截的方法**/
@LoginRequired
```

### 2.编写拦截器类实现HandlerInterceptor父类

```java
  @Autowired
  //注入hostHolder工具类获取当前状态登录用户
  private HostHolder hostHolder;
  
  @Override
  /**在请求路径前执行该方法* */
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      //判断拦截的目标是不是一个方法
      if (handler instanceof HandlerMethod){
          //如果是一个方法，将handler转化我HandlerMethod类型
          HandlerMethod handlerMethod = (HandlerMethod) handler;
          Method method = handlerMethod.getMethod();
          //获取方法上的自定义注解
          LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
         /**
          * 如果没有登录并且有自定义注解（需要登录才能访问的方法注解）
          * 通过response来重定向，这里不可以通过return 重定向
          */
          if (hostHolder.getUser()==null&&loginRequired!=null){
              response.sendRedirect(request.getContextPath() + "/login");
              return false;
          }
      }
      return true;
  }
```

### 3.注册进拦截器配置类WebMvcConfig

```java
  @Autowired
  private LoginRequiredInterceptor loginRequiredInterceptor;
  
   @Override
  public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(loginRequiredInterceptor)
              .excludePathPatterns("/* */*.css","/**/ *.js","/* */*.png","/ **/ *.jpg","/* */*.jpeg");
  }
```

## 修改密码

### 1.编写Dao层

```java
int updatePassword(@Param("id") int id,@Param("password")String password);
<update id="updatePassword">
    update user set password=#{password} where id=#{id}
</update>

```

### 2.编写Service层

```java
  /**修改密码**/
  public Map<String,Object> updatePassword(int userId,String oldPassword,String newPassword){
      HashMap<String, Object> map = new HashMap<>();
  
      // 空值处理
      if (StringUtils.isBlank(oldPassword)) {
          map.put("oldPasswordMsg", "原密码不能为空!");
          return map;
      }
      if (StringUtils.isBlank(newPassword)) {
          map.put("newPasswordMsg", "新密码不能为空!");
          return map;
      }
  
      // 验证原始密码
      User user = userMapper.selectById(userId);
      oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
  
      if (!user.getPassword().equals(oldPassword)){
          map.put("oldPasswordMsg","您输入的原密码错误！");
          return map;
      }
      newPassword = CommunityUtil.md5(newPassword + user.getSalt());
      userMapper.updatePassword(userId,newPassword);
  
      return map;
  }
```

### 3.编写Controller层

```java
  /**修改密码 **/
  @RequestMapping(value = "/updatePassword",method = RequestMethod.POST)
  public String updatePassword(String oldPassword, String newPassword, Model model){
      User user = hostHolder.getUser();
      Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
      if (map == null || map.isEmpty()){
          /**如果更改密码成功，退出登录，并跳到登录页面 **/
          return "redirect:/logout";
      }else{
          model.addAttribute("oldPasswordMsg",map.get("oldPasswordMsg"));
          model.addAttribute("newPasswordMsg",map.get("newPasswordMsg"));
          return "/site/setting";
      }
  }
```

## 忘记密码

### 1.编写Service层

```Java
    // 判断邮箱是否已注册
    public boolean isEmailExist(String email) {
        User user = userMapper.selectByEmail(email);
        return user != null;
    }
    
     /**
      * 重置忘记密码
      */
    public Map<String, Object> resetPassword(String email, String password) {
        HashMap<String, Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        //根据邮箱查找用户
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册!");
            return map;
        }

        //重置密码
        password = CommunityUtil.md5(password + user.getSalt());
        userMapper.updatePassword(user.getId(), password);
        // 清理缓存
        clearCache(user.getId());

        //注意这里！
        map.put("user", user);

        return map;
    }
```

### 2.编写Controller层

```Java
    /**
     * 忘记密码页面
     */
    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }
    
    /**
     * 重置密码
     */
    @RequestMapping(path = "/forget/password", method = RequestMethod.POST)
    public String resetPassword(String email, String verifyCode, String password, Model model, HttpSession session) {
        String code = (String) session.getAttribute(email + "_verifyCode");

        if (StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)) {
            model.addAttribute("codeMsg", "验证码错误!");
            return "/site/forget";
        }

        Map<String, Object> map = userService.resetPassword(email, password);
        if (map.containsKey("user")) {
            return "redirect:/login";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }    
```

### 3.编写前端核心部分

```HTML
  <form method="post" th:action="@{/forget/password}">
      <div>
          <label class="col-sm-2" for="your-email">邮箱:</label>
          <div>
              <input id="your-email" name="email" placeholder="请输入您的邮箱!" required
                     th:class="|form-control ${emailMsg!=null?'is-invalid':''}|" th:value="${param.email}"
                     type="email">
              <div th:text="${emailMsg}">
              <input **id="your-email" name="email" placeholder="请输入您的邮箱!" required
                     th:class="|form-control ${emailMsg!=null?'is-invalid':''}|" th:value="${param.email}"
                     type="email">**
              <div **th:text="${emailMsg}"**>
                  该邮箱已被注册!
              </div>
          </div>
      </div>
      <div >
          <label class="col-sm-2" for="verifycode">验证码:</label>
          <div>
              <input id="verifycode" name="verifyCode" placeholder="请输入验证码!"
                     th:class="|form-control ${codeMsg!=null?'is-invalid':''}|" th:value="${param.verifyCode}"
                     type="text">
              <input **id="verifycode" name="verifyCode" placeholder="请输入验证码!"
                     th:class="|form-control ${codeMsg!=null?'is-invalid':''}|" th:value="${param.verifyCode}"
                     type="text">**
              <div th:text="${codeMsg}">
                  验证码不正确!
              </div>
          </div>
          <div>
              <a class="btn" id="verifyCodeBtn">获取验证码</a>
          </div>
      </div>
      <div>
          <label class="col-sm-2" for="your-password">新密码:</label>
          <div class="col-sm-10">
              <input id="your-password" name="password" placeholder="请输入新的密码!" required
                     th:class="|form-control ${passwordMsg!=null?'is-invalid':''}|"
                     th:value="${param.password}" type="password">
              <input **id="your-password" name="password" placeholder="请输入新的密码!" required
                     th:class="|form-control ${passwordMsg!=null?'is-invalid':''}|"
                     th:value="${param.password}" type="password">**
              <div class="invalid-feedback" th:text="${passwordMsg}">
                  密码长度不能小于8位!
              </div>
          </div>
      </div>
      <button type="submit" class="btn">重置密码</button>
  </form>
```

# 优化登录功能(使用Redis)

## 使用Redis存储验证码

### 1.编写RedisUtil工具类设置验证码key值

```java
public class RedisKeyUtil {
    // 验证码
    private static final String PREFIX_KAPTCHA = "kaptcha";
    /**登录验证码**/
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }
}
```

### 2.优化LoginController验证码相关代码（优化前是存在session中的）

```java
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 验证码功能 (Redis优化)
     * @param response
     */
    @RequestMapping(value = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response) {
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //优化前：将验证码存入session.....

        //优化后：生成验证码的归属传给浏览器Cookie
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        //优化后：将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60 , TimeUnit.SECONDS);

        //将图片输出给浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }
```

```java
    /**
     * 登录功能
     * @param redisKey 用于获取kaptcha验证码
     * @param @CookieValue用于浏览器接受cookie
     * @return
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    /**注意username,password这些没有封装进model**/
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        /**
         * 优化前：首先检验验证码(从session取验证码)
         * String kaptcha = (String) session.getAttribute("kaptcha");
         */

        // 优化后：从redis中获取kaptcha的key
        String kaptcha = null;
        // 判断从浏览器传来的Cookie是否为空
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            // 获取key为验证码的redis数据
            kaptcha  = (String) redisTemplate.opsForValue().get(redisKey);
        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/login";
        }
        /**
         * 1.验证用户名和密码(重点)
         * 2.传入浏览器cookie=ticket
         */
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }
```

## 使用Redis存存登录凭证

### 1.编写RedisUtil工具类设置登录凭证key值

```java
    // 登录凭证
    private static final String PREFIX_TICKET = "ticket";
    /**登录凭证**/
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }
```

### 2.优化UserService中LoginTicket相关代码（废弃LoginTicket数据库表，使用redis）

```java
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 登录功能（redis优化）
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        HashMap<String, Object> map = new HashMap<>();
        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "号码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        //验证激活状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }
        //验证密码(先加密再对比)
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码输入错误！");
            return map;
        }
        //生成登录凭证(相当于记住我这个功能==session)
        LoginTicket ticket = new LoginTicket();
        ticket.setUserId(user.getId());
        ticket.setTicket(CommunityUtil.generateUUID());
        ticket.setStatus(0);
        //当前时间的毫秒数+过期时间毫秒数
        ticket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds*  1000));
        // 优化前：loginTicketMapper.insertLoginTicket(ticket);
        
        // 优化后：loginticket对象放入redis中
        String redisKey = RedisKeyUtil.getTicketKey(ticket.getTicket());
        // opsForValue将ticket对象序列化为json字符串
        redisTemplate.opsForValue().set(redisKey, ticket);

        map.put("ticket", ticket.getTicket());

        return map;
    }
```

```java
    /**
    * 通过Cookie=ticket获取登录用户(redis优化)
    */
    public LoginTicket getLoginTicket(String ticket) {
        //优化前： return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }
```

## 使用Redis缓存用户信息

### 1.编写RedisUtil工具类设置用户缓存key值

```java
    // 用户缓存
    private static final String PREFIX_USER = "user";
    /**用户缓存**/
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }
```

### 2.优化UserService中findUserById和userMapper.updateXXX方法

```java
     /**
     * 因为经常使用这个方法，所以将它用redis缓存优化
     * 若缓存中有访问的用户直接从缓存中取出，否则从数据库查询后加入redis中作为缓存
     */
    public User findUserById(int userId) {
        // return userMapper.selectById(userId);
        // 从redis缓存中取值
        User user = getCache(userId);
        if (user == null) {
            user = initCache(userId);
        }
        return user;
    }
    
    /**
    * 更新头像
    */
    public int updateHeader(int userId, String headerUrl) {
        /** 同时处理mysql和redis事务的方法，报错回滚* */
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }
   
    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2.取不到时初始化缓存数据(redis存值)
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);

        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    // 3.数据变更时清除缓存(删除redis的key)
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
```

# 会话管理（暂时仅有demo）

### 1.面试题Cookie和Session的区别？

1.cookie是存放在浏览器上的，session是存放在服务器上的。

2.cookie数据不安全，如果考虑到安全应使用session。

3.session会增加服务端的内存压力,考虑到减轻服务器性能方面，应当使用cookie。

4.cookie只能存放一对字符串k-v

![](image/cookie_ydwZWF6ZCb.PNG)

![](image/session_4ZmfBdmJQn.PNG)

### 2.Cookie是干嘛的？

因为Http是无状态的，所以需要用到cookie。通俗说cookie是用来让服务器记住浏览器的。

### 3.分布式session共享方案

1、粘性session：在nginx中提供一致性哈希策略，可以保持用户ip进行hash值计算固定分配到某台服务器上，负载也比较均衡，其问题是假如有一台服务器挂了，session也丢失了。

2、同步session：当某一台服务器存了session后，同步到其他服务器中，其问题是同步session到其他服务器会对服务器性能产生影响，服务器之间耦合性较强。

3、共享session：单独搞一台服务器用来存session，其他服务器都向这台服务器获取session，其问题是这台服务器挂了，session就全部丢失。

4、redis集中管理session(主流方法)：redis为内存数据库，读写效率高，并可在集群环境下做高可用。

![](image/Session集群2_6m4V-afro7.PNG)

### 4.简单API实现

```java
/**
 * Cookie示例(获取Cookie时@CookieValue有点问题！！)
 */
@RequestMapping(value = "/cookie/set",method = RequestMethod.GET)
@ResponseBody
public String setCookie(HttpServletResponse response){
    //cookie存的必须是字符串
    Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
    cookie.setPath("/Community/test");
    cookie.setMaxAge(60*10);
    response.addCookie(cookie);

    return "set cookie!";
}

@RequestMapping(value = "/cookie/get",method = RequestMethod.GET)
@ResponseBody
public String getCookie(@CookieValue("code") String code){
    System.out.println(code);
    return "get cookie!";
}

/**
 * Session示例
 */
@RequestMapping(value = "/session/set",method = RequestMethod.GET)
@ResponseBody
public String setSession(HttpSession session){
    session.setAttribute("id",1);
    session.setAttribute("name","xmy");
    return "set session!";
}

@RequestMapping(value = "/session/get",method = RequestMethod.GET)
@ResponseBody
public String getSession(HttpSession session){
    System.out.println(session.getAttribute("id"));
    System.out.println(session.getAttribute("name"));
    return "get session!";
}
```

# 上传头像功能

注意：1. 必须是Post请求 
2.表单：enctype="multipart/form-data"
3.参数类型MultipartFile只能封装一个文件

上传路径可以是本地路径也可以是web路径

访问路径**必须**是符合HTTP协议的**Web路径**

## 1.编写Service和Dao层

```java
//Dao层
<update id="updatePassword">
    update user set password=#{password} where id=#{id}
</update>
int updateHeader(@Param("id") int id,@Param("headerUrl") String headerUrl);

//Service层
/**更换上传头像**/
public int updateHeader(int userId,String headerUrl){
    return userMapper.updateHeader(userId,headerUrl);
}
```

## 2.编程Controller层

```java
@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    //community.path.upload = d:/DemoNowcoder/upload
    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    /**获得当前登录用户的信息* */
    private HostHolder hostHolder;

    @RequestMapping(value = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }
    
    //上传头像
    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
    //StringUtils.isBlank(headerImage)
        if (headerImage == null){
            model.addAttribute("error","您还没有选择图片！");
            return "/site/setting";
        }
        /*
        * 获得原始文件名字
        * 目的是：生成随机不重复文件名，防止同名文件覆盖
        * 方法：获取.后面的图片类型 加上 随机数
        */
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf(".") );

        //任何文件都可以上传,根据业务在此加限制
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确！");
            return "/site/setting";
        }

        //生成随机文件名
        filename = CommunityUtil.generateUUID() + suffix;
        //确定文件存放路劲
        File dest = new File(uploadPath + "/" +filename);
        try{
            //将文件存入指定位置
            headerImage.transferTo(dest);
        }catch (IOException e){
            logger.error("上传文件失败： "+ e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！",e);
        }
        //更新当前用户的头像的路径（web访问路径）
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(),headerUrl);

        return "redirect:/index";
    }
```

```java
  //得到服务器图片
  @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
  /**void:返回给浏览器的是特色的图片类型所以用void**/
  public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
      // 服务器存放路径(本地路径)
      fileName = uploadPath + "/" + fileName;
      // 文件后缀
      String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
      // 浏览器响应图片
      response.setContentType("image/" + suffix);
      try (
              //图片是二进制用字节流
              FileInputStream fis = new FileInputStream(fileName);
              OutputStream os = response.getOutputStream();
      ) {
          //设置缓冲区
          byte[] buffer = new byte[1024];
          //设置游标
          int b = 0;
          while ((b = fis.read(buffer)) != -1) {
              os.write(buffer, 0, b);
          }
      } catch (IOException e) {
          logger.error("读取头像失败: " + e.getMessage());
      }
  }
```

## 3.前端核心页面

```html
<form class="mt-5" method="post" enctype="multipart/form-data" th:action="@{/user/upload}">
    <div class="custom-file">
      <input type="file"
           th:class="|custom-file-input ${error!=null?'is-invalid':''}|"
           name="headerImage" id="head-image" lang="es" required="">
      <label class="custom-file-label" for="head-image" data-browse="文件">选择一张图片</label>
      <div class="invalid-feedback" th:text="${error}">
        该账号不存在!
      </div>
    </div>
</form>
```

# 过滤敏感词

前缀树  ：1.根节点不包含字符，除根节点以外的每个节点，只包含一个字符

&#x20;        2.从根节点到某一个节点，路径上经过的字符连接起来，为该节点对应字符串

&#x20;    3.每个节点的所有子节点，包含的字符串不相同

核心  ：1.有一个指针指向前缀树，用以遍历敏感词的每一个字符

&#x20;         2.有一个指针指向被过滤字符串，用以标识敏感词的开头

&#x20;         3.有一个指针指向被过滤字符串，用以标识敏感词的结尾

![](image/前缀树_nTNaIPnorr.PNG)

### 1.过滤敏感词算法

**在resources创建sensitive-words.txt文敏感词文本**

```java
/**
 * 过滤敏感词工具类
 * 类似于二叉树的算法
 */
@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "* **";

    // 根节点
    private TrieNode rootNode = new TrieNode();

    // 编译之前运行
    @PostConstruct
    public void init() {
        try (
                // 读取文件流 BufferedReader带缓冲区效率更高
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            // 一行一行读取文件中的字符
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败: " + e.getMessage());
        }
    }
    /**
     * 将一个敏感词添加到前缀树中
     * 类似于空二叉树的插入
     */
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            //将汉字转化为Char值
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                // 初始化子节点并加入到前缀树中
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向子节点,进入下一轮循环
            tempNode = subNode;

            // 设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果(StringBuilder：可变长度的String类)
        StringBuilder sb = new StringBuilder();

        while (position < text.length()) {
            char c = text.charAt(position);
            // 跳过符号
            if (isSymbol(c)) {
                // 若指针1处于根节点,将此符号计入结果,让指针2向下走一步
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间,指针3都向下走一步
                position++;
                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                // 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = rootNode;
            } else {
                // 检查下一个字符
                position++;
            }
        }

        // 将最后一批字符计入结果
        sb.append(text.substring(begin));

        return sb.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    // 构造前缀树数据结构
    private class TrieNode {

        // 关键词结束标识
        private boolean isKeywordEnd = false;

        // 子节点(key是下级字符,value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
```

### 2.引入第三方Maven,如下：

[*https://github.com/jinrunheng/sensitive-words-filter* ](https://github.com/jinrunheng/sensitive-words-filter "https://github.com/jinrunheng/sensitive-words-filter")

```xml
<dependency>
  <groupId>io.github.jinrunheng</groupId>
  <artifactId>sensitive-words-filter</artifactId>
  <version>0.0.1</version>
</dependency>
```

# 发布贴子

核心 **：ajax异步：整个网页不刷新，访问服务器资源返回结果，实现局部的刷新。**

实质：**JavaScript**和XML（但目前**JSON**的使用比XML更加普遍）

封装**Fastjson**工具类

```javascript
  //使用fastjson，将JSON对象转为JSON字符串(前提要引入Fastjson)
  public static String getJSONString(int code, String msg, Map<String, Object> map) {
      JSONObject json = new JSONObject();
      json.put("code",code);
      json.put("msg",msg);
      if (map != null) {
          //从map里的key集合中取出每一个key
          for (String key : map.keySet()) {
              json.put(key, map.get(key));
          }
      }
      return json.toJSONString();
  }
  public static String getJSONString(int code, String msg) {
    return getJSONString(code, msg, null);
  }
  public static String getJSONString(int code) {
    return getJSONString(code, null, null);
  }
```

### ajax异步Demo示例

```java
  /**
   * Ajax异步请求示例
   */
  @RequestMapping(value = "/ajax", method = RequestMethod.POST)
  @ResponseBody
  public String testAjax(String name, int age) {
      System.out.println(name);
      System.out.println(age);
      return CommunityUtil.getJSONString(200,"操作成功！");
  }
```

```javascript
  //异步JS
  <input type="button" value="发送" onclick="send();">
  function send() {
      $.post(
          "/community/test/ajax",
          {"name":"张三","age":25},
          //回调函数返回结果
          function(data) {
              console.log(typeof (data));
              console.log(data);
              //返回json字符串格式(fastJson)
              data = $.parseJSON(data);
              console.log(typeof (data));
              console.log(data.code);
              console.log(data.msg);
          }
      )
  }

```

## 1.编写Mapper层

```xml
int insertDiscussPost(DiscussPost discussPost);

<sql id="insertFields">
    user_id,title,content,type,status,create_time,comment_count,score
</sql>
<insert id="insertDiscussPost" parameterType="DiscussPost">
    insert into discuss_post(<include refid="insertFields"></include>)
    values (#{userId}, #{title}, #{content}, #{type}, #{status}, #{createTime}, #{commentCount}, #{score})
</insert>
```

## 2.编写Service层

```java
  public int addDiscussPost(DiscussPost post){
      if(post == null){
          //不用map直接抛异常
          throw new IllegalArgumentException("参数不能为空！");
      }
      //转义HTML标签,Springboot自带转义工具HtmlUtils.htmlEscape()
      post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
      post.setContent(HtmlUtils.htmlEscape(post.getContent()));
      //过滤敏感词
      post.setTitle(sensitiveFilter.filter(post.getTitle()));
      post.setContent(sensitiveFilter.filter(post.getContent()));

      return discussPostMapper.insertDiscussPost(post);
  }

```

## 3.编写Controller层(异步请求要加@ResponseBody,且不用在Controller层用Model，用Js)

```java
  @Autowired
  private DiscussPostService discussPostService;
  @Autowired
  private HostHolder hostHolder;
  
  @RequestMapping(value = "/add", method = RequestMethod.POST)
  @ResponseBody    //返回Json格式，一定要加@ResponseBody
  public String addDiscussPost(String title, String content){
      //获取当前登录的用户
      User user = hostHolder.getUser();
      if (user == null){
          //403权限不够
          return CommunityUtil.getJSONString(403,"你还没有登录哦！");
      }
      DiscussPost post = new DiscussPost();
      post.setUserId(user.getId());
      post.setTitle(title);
      post.setContent(content);
      post.setCreateTime(new Date());
      //业务处理，将用户给的title，content进行处理并添加进数据库
      discussPostService.addDiscussPost(post);

      //返回Json格式字符串给前端JS,报错的情况将来统一处理
      return CommunityUtil.getJSONString(0,"发布成功！");
  }
```

## 4.编写前端异步JS

注意：\$.parseJSON(data) →通过jQuery，将服务端返回的JSON格式的字符串转为js对象

```javascript
$(function(){
  $("#publishBtn").click(publish);
});

function publish() {
  $("#publishModal").modal("hide");
  /**
  * 服务器处理
  */
  // 获取标题和内容
  var title = $("#recipient-name").val();
  var content = $("#message-text").val();
  // 发送异步请求(POST)
  $.post(
    CONTEXT_PATH + "/discuss/add",
    //与Controller层两个属性要一致！！！
    {"title":title,"content":content},
    function(data) {
      //把json字符串转化成Js对象,后面才可以调用data.msg
      data = $.parseJSON(data);
      // 在提示框中显示返回消息
      $("#hintBody").text(data.msg);
      // 显示提示框
      $("#hintModal").modal("show");
      // 2秒后,自动隐藏提示框
      setTimeout(function(){
        $("#hintModal").modal("hide");
        // 刷新页面
        if(data.code == 0) {
          window.location.reload();
        }
      }, 2000);
    }
  );
}
```

# 查看帖子详情

## 1.编写Mapper层

```xml
DiscussPost selectDiscussPostById(int id);
<---------------------->
<select id="selectDiscussPostById" resultType="DiscussPost">
    select <include refid="selectFields"></include>
    from discuss_post
    where id = #{id}
</select>
```

## 2.编写Service层

```java
  public DiscussPost findDiscussPostById(int id){
      return discussPostMapper.selectDiscussPostById(id);
  }
```

## 3.编写Controller层

```java
  @RequestMapping(value = "/detail/{discussPostId}", method = RequestMethod.GET)
  public String getDiscusspost(@PathVariable("discussPostId") int discussPostId, Model model){
      //通过前端传来的Id查询帖子
      DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
      model.addAttribute("post",post);

      //用以显示发帖人的头像及用户名
      User user = userService.findUserById(post.getUserId());
      model.addAttribute("user",user);
      return "/site/discuss-detail";
  }
```

## 4.编写前端核心部分（进入详情链接及Controller层中的model）

```html
<!--前端点击进入详情的链接-->
<li th:each="map:${discussPosts}">
  <a th:href="@{|/discuss/detail/${map.post.id}|}" th:utext="${map.post.title}">标题链接</a>
</li>

th:utext="${post.getTitle()}"     <!--标题-->
th:src="${user.getHeaderUrl()}"   <!--用户头像-->
th:utext="${user.getUsername()}"  <!--用户名字-->
th:text="${#dates.format(post.getCreateTime(),'yyyy-MM-dd HH:mm:ss')}"   <!--发帖时间-->
th:utext="${post.getContent()}"   <!--发帖内容-->
```

# 事务管理

## 1.概念

### 1.1事务的特性

原子性：**即事务是应用中不可再分的最小执行体。**

一致性：**即事务执行的结果，必须使数据从一个一致性状态，变为另一个一致性状态。**

隔离性：**即各个事务的执行互不干扰，任何事务的内部操作对其他的事务都是隔离的。**

持久性：**事务一旦提交，对数据所做的任何改变都要记录到永久存储器。**

### 1.2事务的四种隔离级别

Read Uncommitted： 读未提交（级别**最低**）

Read Committed： 读已提交

Repeatable Read： 可重复读

Serializable： 串行化（级别**最高** ，*性能最低，因为要加锁）*

### 1.3并发异常

- 第一类丢失更新

- 第二类丢失更新

- 脏读

- 不可重复读

- 幻读

![](image/3_Mbdb-PY0NL.PNG)

![](image/4_YlSXH6_OBG.PNG)

![](image/5_a3J6VuhqzZ.PNG)

![](image/6_4dy3KJ0Wtd.PNG)

![](image/7_B6xJyOOtTx.PNG)

![](image/8_Nd_jlUfSXc.PNG)

![](image/9_hSNFRdQQ1L.PNG)

## 2.Spring声明式事务

 方法： **1.通过XML配置    2.通过注解@Transaction，如下：**

```java
/* REQUIRED: 支持当前事务（外部事务），如果不存在则创建新事务
 * REQUIRED_NEW: 创建一个新事务，并且暂停当前事务（外部事务）
 * NESTED: 如果当前存在事务（外部事务），则嵌套在该事务中执行（独立的提交和回滚），否则就会和REQUIRED一样
 * 遇到错误，Sql回滚  （A->B）
 */
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
```

## 3.Spring编程式事务(通常用来管理中间某一小部分事务)

**方法：** **通过TransactionTemplate组件执行SQL管理事务，如下：**

```java
  public Object save2(){
      transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
      transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
      
      return transactionTemplate.execute(new TransactionCallback<Object>() {
          @Override
          public Object doInTransaction(TransactionStatus status) {
              User user = new User();
              user.setUsername("Marry");
              user.setSalt(CommunityUtil.generateUUID().substring(0,5));
              user.setPassword(CommunityUtil.md5("123123")+user.getSalt());
              user.setType(0);
              user.setHeaderUrl("http://localhost:8080/2.png");
              user.setCreateTime(new Date());
              userMapper.insertUser(user);
              //设置error,验证事务回滚
              Integer.valueOf("abc");
              return "ok"; }
      });
 }
```

# 评论功能

## 显示评论（评论和评论中的回复）

### 1.编写Dao层接口

```java
  /**
   * 根据评论类型(帖子评论和回复评论)和评论Id--分页查询评论
   * @return Comment类型集合
   */
  List<Comment> selectCommentsByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId,
                                       @Param("offset") int offset, @Param("limit") int limit);

  int selectCountByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId);
  <!------------------Mapper.xml----------------------> 
    <select id="selectCommentsByEntity" resultType="Comment">
        select <include refid="selectFields"></include>
        from comment
        where status = 0
        and entity_type = #{entityType}
        and entity_Id = #{entityId}
        order by create_time asc
        limit  #{offset}, #{limit}
    </select>

    <select id="selectCountByEntity" resultType="int">
        select count(id)
        from comment
        where status = 0
        and entity_type = #{entityType}
        and entity_id = #{entityId}
    </select>
  
```

### 2.编写业务Service层

```java
  public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit){
      return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
  }
  public int findCommentCount(int entityType, int entityId){
      return commentMapper.selectCountByEntity(entityType, entityId);
  }
```

### 3.编写Controller控制层（接查看帖子详情，如上）难点（类似于套娃）！

```java
  @RequestMapping(value = "/detail/{discussPostId}", method = RequestMethod.GET)
  public String getDiscusspost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
      //通过前端传来的Id查询帖子
      DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
      model.addAttribute("post", post);

      //查询发帖人的头像及用户名
      User user = userService.findUserById(post.getUserId());
      model.addAttribute("user", user);
      
      // 点赞数量
      long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
      model.addAttribute("likeCount", likeCount);

      // 点赞状态 (没登录就显示0)
      int likeStatus = hostHolder.getUser() == null ? '0' : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
      model.addAttribute("likeStatus", likeStatus);

      //设置评论分页信息
      page.setLimit(3);
      page.setPath("/discuss/detail/"+discussPostId);
      page.setRows(post.getCommentCount());
      
      // 评论: 给帖子的评论
      // 回复: 给评论的评论
      // 评论列表集合
      List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

      // 评论VO(viewObject)列表 (将comment,user信息封装到每一个Map，每一个Map再封装到一个List中)
      List<Map<String, Object>> commentVoList = new ArrayList<>();
      if (commentList != null){
          // 每一条评论及该评论的用户封装进map集合
          for (Comment comment : commentList){
              // 评论Map-->commentVo
              HashMap<String, Object> commentVo = new HashMap<>();
              // 评论
              commentVo.put("comment", comment);
              // 作者(由comment表中 entity = 1 查user表)
              commentVo.put("user", userService.findUserById(comment.getUserId()));
              
              // 点赞数量
              likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
              commentVo.put("likeCount", likeCount);
              // 点赞状态 (没登录就显示0)
              likeStatus = hostHolder.getUser() == null ? '0' : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
              commentVo.put("likeStatus", likeStatus);

              // 回复列表集合（每一条评论的所有回复,不分页）
              List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);

              // 回复VO
              List<Map<String, Object>> replyVoList = new ArrayList<>();
              if (replyList !=null){
                  for (Comment reply : replyList){
                      // 回复Map
                      HashMap<String, Object> replyVo = new HashMap<>();
                      // 回复
                      replyVo.put("reply", reply);
                      // 作者 (由comment表中 entity = 2 查user表)
                      replyVo.put("user", userService.findUserById(reply.getUserId()));
                      // 回复目标 (有2种：1.直接回复 2.追加回复)
                      User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                      replyVo.put("target", target);
                      
                      // 点赞数量
                      likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                      replyVo.put("likeCount", likeCount);
                      // 点赞状态 (没登录就显示0)
                      likeStatus = hostHolder.getUser() == null ? '0' : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                      replyVo.put("likeStatus", likeStatus);
                      
                      // 将每一个回复Map放在回复List中
                      replyVoList.add(replyVo);
                  }
              }
              // 将每一个回复List放在评论Map中
              commentVo.put("replys", replyVoList);

              // 回复数量统计
              int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
              commentVo.put("replyCount", replyCount);

              // 再将每一个评论Map放在评论List中
              commentVoList.add(commentVo);
          }
      }
      // 最后将整个List传给前端model渲染
      model.addAttribute("comments", commentVoList);

      return "/site/discuss-detail";
  }
```

### 4.编写前端Thymeleaf页面（核心部分）

注意： xxxStat—>Thymeleaf内置对象

```html
<!-- 回帖列表 -->
<li class="media pb-3 pt-3 mb-3 border-bottom" th:each="cvo:${comments}">
  <img th:src="${cvo.user.getHeaderUrl()}" alt="用户头像">
  <div>
    <span th:utext="${cvo.user.getUsername()}">用户姓名</span>
    <span>
      <i th:text="${page.offset + cvoStat.count}">1 评论楼层</i>#
    </span>
  </div>
  <div th:utext="${cvo.comment.content}">
    评论内容
  </div>
  <span>发布于 <b th:text="${#dates.format(cvo.comment.createTime,'yyyy-MM-dd HH:mm:sss')}">时间</b></span>
  <ul>
    <li><a href="#">回复(<i th:text="${cvo.replyCount}">2</i>)</a></li>
  </ul>
  
  <!-- 回复列表 -->
  <li th:each="rvo:${cvo.replys}">
    <div>
      <!--直接回复-->
      <span th:if="${rvo.target==null}">
        <b th:utext="${rvo.user.username}">回复人姓名</b>
      </span>
      <!--追加回复-->
      <span th:if="${rvo.target!=null}">
        <i th:text="${rvo.user.username}">回复人姓名</i> 回复
        <b th:text="${rvo.target.username}">被回复人姓名</b>
      </span>
      <span th:utext="${rvo.reply.content}">回复内容</span>
    </div>
    
    <div>
      <span th:text="${#dates.format(rvo.reply.createTime,'yyyy-MM-dd HH:mm:ss')}">回复时间</span>
      <ul>
        <li><a href="#">赞(1)</a></li>
        <li>|</li>
        <!--关联id对应回复  动态拼接-->
        <li><a th:href="|#huifu-${rvoStat.count}|" data-toggle="collapse">回复</a></li>
      </ul>
      
      <div th:id="|huifu-${rvoStat.count}|">
        <input type="text" th:placeholder="|回复${rvo.user.username}|"/>
        <button type="button" onclick="#">回复</button>                   
      </div>
    </div>                
  </li>
</li>
最后复用分页：th:replace="index::pagination"
```

## 添加评论  (用到事务管理)

### 1.编写Dao层 （1.增加评论数据CommentMapper 2.修改帖子评论数量DiscussPostMapper）

```java
 //CommentMapper 
 int insertComment(Comment comment);
 <insert id="insertComment" parameterType="Comment">
    insert into comment(<include refid="insertFields"></include>)
    values (#{userId}, #{entityType}, #{entityId}, #{targetId}, #{content}, #{status}, #{createTime})
 </insert>
 
 //DiscussPostMapper
 int updateCommentCount(@Param("id") int id,@Param("commentCount") int commentCount);
 <update id="updateCommentCount">
    update discuss_post set comment_count = #{commentCount}
    where id = #{id}
 </update>

```

### 2.编写业务Service层

```java
  //DiscussPostService
   public int updateCommentCount(int id, int commentCount){
      return discussPostMapper.updateCommentCount(id, commentCount);
   }
   
  //CommentService
  /**
   * 添加评论(涉及事务)
   * 先添加评论，后修改discuss_post中的评论数（作为一个整体事务，出错需要整体回滚！）
   */
  @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
  public int addComment(Comment comment){
      if (comment == null){
          throw new IllegalArgumentException("参数不能为空！");
      }
      /**添加评论**/
      //过滤标签
      comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
      //过滤敏感词
      comment.setContent(sensitiveFilter.filter(comment.getContent()));
      int rows =commentMapper.insertComment(comment);
      /**
       * 更新帖子评论数量
       * 如果是帖子类型才更改帖子评论数量，并且获取帖子评论的id
       */
      if (comment.getEntityType() == ENTITY_TYPE_POST){
          int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
          discussPostService.updateCommentCount(comment.getEntityId(), count);
      }
      return rows;
  }
```

### 3.编写Controller层

```java
  //需要从前端带一个参数
  @RequestMapping(value = "/add/{discussPostId}", method = RequestMethod.POST)
  public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
      comment.setUserId(hostHolder.getUser().getId());
      comment.setStatus(0);
      comment.setCreateTime(new Date());
      commentService.addComment(comment);
      return "redirect:/discuss/detail/" + discussPostId;
  }
```

### 4.编写Thymleaf前端页面（核心）

```html
<!--帖子评论框-->
<form method="post" th:action="@{|/comment/add/${post.id}|}">
  <p>
    <textarea placeholder="帖子评论框!" name="content"></textarea>
    <input type="hidden" name="entityType" value="1">
    <input type="hidden" name="entityId" th:value="${post.id}">
  </p>
  <button type="submit">回帖</button>
</form>

<!--回复输入框-->
<form method="post" th:action="@{|/comment/add/${post.id}|}">
  <div>
    <input type="text" name="content" placeholder="回复输入框"/>
    <input type="hidden" name="entityType" value="2">
    <!--回复评论id，即entityType=2的评论id-->
    <input type="hidden" name="entityId" th:value="${cvo.comment.id}">
  </div>
  <button type="submit" onclick="#">回复</button>
</form>

<!--追加回复框-->
<form method="post" th:action="@{|/comment/add/${post.id}|}">
  <div>
    <input type="text" name="content" th:placeholder="|回复${rvo.user.username}|"/>
    <input type="hidden" name="entityType" value="2">
    <input type="hidden" name="entityId" th:value="${cvo.comment.id}">
    <!--回复评论的作者id-->
    <input type="hidden" name="targetId" th:value="${rvo.user.id}">
  </div>
  <button type="submit" onclick="#">回复</button>
</form>
```

# 私信功能

## 显示私信列表（难度在写SQL）

### 1.编写Dao层

```java
  /**查询当前用户的会话列表,针对每个会话只返回一条最新的私信**/
  List<Message> selectConversations(@Param("userId") int userId,@Param("offset") int offset,@Param("limit") int limit);

  /**查询当前用户的会话数量**/
  int selectConversationCount(@Param("userId") int userId);

  /**查询某个会话所包含的私信列表**/
  List<Message> selectLetters(@Param("conversationId") String conversationId,@Param("offset") int offset,@Param("limit") int limit);

  /**查询某个会话所包含的私信数量**/
  int selectLetterCount(@Param("conversationId") String conversationId);
  /**
   * 查询未读的数量
   * 1.带参数conversationId ：私信未读数量
   * 2.不带参数conversationId ：当前登录用户 所有会话未读数量
   */
  int selectLetterUnreadCount(@Param("userId")int userId,@Param("conversationId") String conversationId);
```

### 2.编写Mapper.xml(难度)

```sql
<sql id="selectFields">
    id, from_id, to_id, conversation_id, content, status, create_time
</sql>

<select id="selectConversations" resultType="Message">
    select <include refid="selectFields"></include>
    from message
    where id in (
        //子句根据id大小查与每个用户最新的私信（同一会话id越大，私信越新）
        //也可根据时间戳判断
        select max(id) from message
        where status != 2
        and from_id != 1
        and (from_id = #{userId} or to_id = #{userId})
        group by conversation_id   //同一会话只显示一条
    )
    order by id desc
    limit #{offset}, #{limit}
</select>

<select id="selectConversationCount" resultType="int">
    select count(m.maxid) from (
       select max(id) as maxid from message
       where status != 2
       and from_id != 1
       and (from_id = #{userId} or to_id = #{userId})
       group by conversation_id
   ) as m
</select>

<select id="selectLetters" resultType="Message">
    select <include refid="selectFields"></include>
    from message
    where status != 2
    and from_id != 1
    and conversation_id = #{conversationId}
    order by id asc
    limit #{offset}, #{limit}
</select>

<select id="selectLetterCount" resultType="int">
    select count(id)
    from message
    where status != 2
    and from_id != 1
    and conversation_id = #{conversationId}
</select>

<select id="selectLetterUnreadCount" resultType="int">
    select count(id)
    from message
    where status = 0
    and from_id != 1
    and to_id = #{userId}
    <if test="conversationId!=null"> //=null:所有会话未读数 !=null:每条会话未读数
        and conversation_id = #{conversationId}
    </if>
</select>
```

### 3.编写Service层

```java
  @Autowired
  private MessageMapper messageMapper;

  public List<Message> findConversations(int userId, int offset, int limit){
      return messageMapper.selectConversations(userId, offset, limit);
  }
  public int findConversationCount(int userId) {
      return messageMapper.selectConversationCount(userId);
  }
  public List<Message> findLetters(String conversationId, int offset, int limit) {
      return messageMapper.selectLetters(conversationId, offset, limit);
  }
  public int findLetterCount(String conversationId) {
      return messageMapper.selectLetterCount(conversationId);
  }
  public int findLetterUnreadCount(int userId, String conversationId) {
      return messageMapper.selectLetterUnreadCount(userId, conversationId);
  }
```

### 4.编写Controller层

#### 4.1私信列表Controller

```java
  /**私信列表**/
  @RequestMapping(value = "/letter/list", method = RequestMethod.GET)
  public String getLetterList(Model model, Page page){
      // 获取当前登录用户
      User user = hostHolder.getUser();
      // 分页信息
      page.setLimit(5);
      page.setPath("/letter/list");
      page.setRows(messageService.findConversationCount(user.getId()));
      // 会话列表
      List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
      List<Map<String, Object>> conversations = new ArrayList<>();
      if (conversationList != null){
          for (Message message : conversationList){
              HashMap<String, Object> map = new HashMap<>();
              // 与当前登录用户每一条会话的所有信息
              map.put("conversation", message);
              // 当前登录用户与每一个会话人的私信条数
              map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
              // 当前登录用户与每一个会话人的未读条数
              map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
              // 当前登录用户若与当前会话信息中fromId相同，则目标id为ToId;
              int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
              User target = userService.findUserById(targetId);
              map.put("target", target);
              
              conversations.add(map);
          }
      }
      model.addAttribute("conversations", conversations);
      // 当前登录用户总未读条数
      int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
      model.addAttribute("letterUnreadCount", letterUnreadCount);

      return "/site/letter";
  }
```

#### 4.2私信详情Controller

```java
  /**私信详情**/
  @RequestMapping(value = "/letter/detail/{conversationId}", method = RequestMethod.GET)
  public String getLetterDetail(@PathVariable("conversationId")String conversationId, Model model, Page page){
      //分页信息
      page.setLimit(5);
      page.setPath("/letter/detail/"+conversationId);
      page.setRows(messageService.findLetterCount(conversationId));

      //获取私信信息
      List<Message> letterlist = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
      List<Map<String, Object>> letters = new ArrayList<>();
      if (letterlist != null){
          for(Message message : letterlist){
              HashMap<String, Object> map = new HashMap<>();
              //map封装每条私信
              map.put("letter", message);
              map.put("fromUser",userService.findUserById(message.getFromId()));

              letters.add(map);
          }
      }
      model.addAttribute("letters",letters);
      //私信目标
      model.addAttribute("target",getLetterTarget(conversationId));
      return "/site/letter-detail";
  }

  /**封装获取目标会话用户(将如：101_107拆开) **/
  private User getLetterTarget(String conversationId) {
      String[] ids = conversationId.split(" _");
      int id0 = Integer.parseInt(ids[0]);
      int id1 = Integer.parseInt(ids[1]);

      if (hostHolder.getUser().getId() == id0) {
          return userService.findUserById(id1);
      } else {
          return userService.findUserById(id0);
      }
  }

```

### 5.编写Thymeleaf前端页面（核心）

#### 5.1私信列表页面

```html
<a th:href="@{/letter/list}">
  朋友私信<span th:text="${letterUnreadCount}" th:if="${letterUnreadCount!=0}">总私信未读数</span>
</a>

<li th:each="map:${conversations}">
  <span th:text="${map.unreadCount}" th:if="${map.unreadCount!=0}">单个会话未读数</span>
  <a th:href="@{/profile}">
    <img th:src="${map.target.headerUrl}" alt="用户头像" >
  </a>
  <div>
    <span th:utext="${map.target.username}">会话目标姓名</span>
    <span th:text="${#dates.format(map.conversation.createTime,'yyyy-MM-dd HH:mm:ss')}">会话最新时间</span>
    <a th:href="@{|/letter/detail/${map.conversation.conversationId}|}" th:utext="${map.conversation.content}">会话内容，可进入详情页</a>
    <ul>
      <li><a href="#">共<i th:text="${map.letterCount}">5</i>条会话</a></li>
    </ul>
  </div>
</li>
```

#### 5.2私信详情页面

```html
<h6>来自 <i th:utext="${target.username}">目标会话用户</i> 的私信</h6>

<li th:each="map:${letters}">
  <img th:src="${map.fromUser.headerUrl}" alt="用户头像" >
<div>
  <strong th:utext="${map.fromUser.username}">会话发起人姓名</strong>
  <small th:text="${#dates.format(map.letter.createTime,'yyyy-MM-dd HH:mm:ss')}">时间</small>
</div>
<div th:utext="${map.letter.content}">
   私信内容
</div>
</li>

```

## 发送私信功能（异步）

### 1.编写Dao层

```sql
  /**插入会话**/
  int insertMessage(Message message);
  /**批量更改每个会话的所有未读消息为已读**/
  int updateStatus(@Param("id") List<Integer> ids,@Param("status") int status);
  
  -----------------------Mapper.xml-----------------------------
  <insert id="insertMessage" parameterType="Message" keyProperty="id">
    insert into message(<include refid="insertFields"></include>)
    values(#{fromId},#{toId},#{conversationId},#{content},#{status},#{createTime})
  </insert>
  
  <update id="updateStatus">
      update message set status = #{status}
      where id in
      -----批量传入id写法
      <foreach collection="ids" item="id" open="(" separator="," close=")">
          #{id}
      </foreach>
  </update>
  
```

### 2.编写Service层

```java
  public int addMessage(Message message){
      //转义标签
      message.setContent(HtmlUtils.htmlEscape(message.getContent()));
      //过滤敏感词
      message.setContent(sensitiveFilter.filter(message.getContent()));
      return messageMapper.insertMessage(message);
  }
  public int readMessage(List<Integer> ids){
      return messageMapper.updateStatus(ids, 1);
  }
```

### 3.编写Controller层

#### 3.1设置已读

```java
  @RequestMapping(value = "/letter/detail/{conversationId}", method = RequestMethod.GET)
  public String getLetterDetail(@PathVariable("conversationId")String conversationId, Model model, Page page){
        /**
        * 以上省略。。。。。。
        */
        //设置已读(当打开这个页面是就更改status =1)
        List<Integer> ids = getLetterIds(letterlist);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
       }
   }

  /**获得批量私信的未读数id* */
  private List<Integer> getLetterIds(List<Message> letterList){
      List<Integer> ids = new ArrayList<>();

      if (letterList != null) {
          for (Message message : letterList) {
              //只有当前登录用户与message列表中目标用户一致并且staus = 0 时才是未读数，加入未读私信集合
              if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                  ids.add(message.getId());
              }
          }
      }
      return ids;
  }
```

#### 3.2 发送私信

```java
  /**发送私信* */
  @RequestMapping(value = "/letter/send", method = RequestMethod.POST)
  @ResponseBody
  public String sendLetter(String toName, String content){
      //根据目标发送人姓名获取其id
      User target = userService.findUserByName(toName);
      if (target == null){
          return CommunityUtil.getJSONString(1,"目标用户不存在!");
      }

      //设置message属性
      Message message = new Message();
      message.setFromId(hostHolder.getUser().getId());
      message.setToId(target.getId());
      message.setContent(content);
      message.setCreateTime(new Date());
      // conversationId (如101_102: 小_大)
      if (message.getFromId() < message.getToId()) {
          message.setConversationId(message.getFromId() + " _" +message.getToId());
      }else{
          message.setConversationId(message.getToId() + "_" +message.getFromId());
      }
      messageService.addMessage(message);

      return CommunityUtil.getJSONString(0);
  }
```

### 4.编写前端JS异步请求（ajax）

```javascript
function send_letter() {
  $("#sendModal").modal("hide");
  //若用JS异步请求，前端参数不用name= "xxx",用如下方法
  var toName = $("#recipient-name").val();
  var content = $("#message-text").val();

  $.post(
    // 接口路径(与@RequestMapping(value = "/letter/send", method = RequestMethod.POST)路径一致)
    CONTEXT_PATH + "/letter/send",
    // 接口参数(与public String sendLetter(String toName, String content)参数一致)
    {"toName":toName, "content":content},
    function (data) {
      // 把{"toName":toName, "content":content}转换成JS对象
      data = $.parseJSON(data);
      // 与CommunityUtil.getJSONString(0,"msg")匹配--0：成功
      if (data.code == 0){
        $("#hintBody").text("发送成功！");
      }else {
        $("#hintBody").text(data.msg);
      }

      $("#hintModal").modal("show");
      setTimeout(function(){
        $("#hintModal").modal("hide");
        //刷新页面
        location.reload();
      }, 2000);
    }
  );
}
```

# 点赞功能（Redis+异步ajax）

## 点赞、取消点赞

&#x20;   注意：**1引入pom,配置Yaml**

&#x20;         2.因为访问的是Redis，无需编写Dao层

### 1.创建RedisKeyUtil工具类(统一格式化redis的key)

k:v = like:entity:entityType:entityId -> set(userId)

```java
    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    /**
    * 某个实体的赞
    * key= like:entity:entityType:entityId -> value= userId
    */
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }
```

### 2.直接编写Service业务层

```java
    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞 (记录谁点了哪个类型哪个留言/帖子id)
    public void like(int userId, int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //判断like:entity:entityType:entityId 是否有对应的 userId
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);

        // 第一次点赞，第二次取消点赞
        if (isMember){
            // 若已被点赞(即entityLikeKey里面有userId)则取消点赞->将userId从中移除
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
        }else {
            redisTemplate.opsForSet().add(entityLikeKey, userId);
        }
    }

    // 查询某实体(帖子、留言)点赞的数量 --> scard like:entity:1:110
    public long findEntityLikeCount(int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 显示某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        // 1：已点赞 , 0：赞
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }
```

### 3.编写点赞Controller层接口（异步）

返回：**CommunityUtil.getJSONString(0,null, map)  —>对应响应的js的ajax**

```java
@Controller
public class LikeController {

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;

    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId){
        User user = hostHolder.getUser();
        // 点赞
        likeService.like(user.getId(), entityType ,entityId);
        // 获取对应帖子、留言的点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 获取当前登录用户点赞状态（1：已点赞 0：赞）
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        // 封装结果到Map
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        return CommunityUtil.getJSONString(0,null, map);  
    }
}
```

### 4.编写异步js

```javascript
// btn -->对应this
function like(btn, entityType, entityId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            }else {
                alert(data.msg);
            }
        }
    );
}
```

### 5.前端—详情页点赞数量

对应的Controll层，显示点赞在****主页Controller层****及\ *\ *显示评论功能Controller层**

```html
  <!--引入Js-->
  <script th:src="@{/js/discuss.js}"></script>
  
  <!---href="javascript:;"弃用href使用onclick按钮
   th:onclick="|like(this,1,${post.id})|", this指代当前按钮,1指代帖子类型--->
  <!--帖子点赞-->
  <a href="javascript:;" th:onclick="|like(this,1,${post.id});|" class="text-primary">
    <b th:text="${likeStatus==1?'已赞':'赞'}">赞</b> <i th:text="${likeCount}">11</i>
  </a>
  
  <!--评论点赞-->
  <a href="javascript:;" th:onclick="|like(this,2,${cvo.comment.id});|" class="text-primary">
    <b th:text="${cvo.likeStatus==1?'已赞':'赞'}">赞</b>(<i th:text="${cvo.likeCount}">1</i>)
  </a>
  
  <!--回复点赞-->
  <a href="javascript:;" th:onclick="|like(this,2,${rvo.reply.id});|" class="text-primary">
    <b th:text="${rvo.likeStatus==1?'已赞':'赞'}">赞</b>(<i th:text="${rvo.likeCount}">1</i>)
  </a>
```

## 我收到的赞（基于点赞基础上修改）

 注意：**1. 以用户为key, 记录点赞数量      2.opsForValue.increment(key) /decrement(key)**

### 1.在工具类RedisKeyUtil添加方法

**k:v =** **like:user:userId -> set(int)**

```java
    private static final String PREFIX_USER_LIKE = "like:user";
    
    /**
     * 某个用户的赞
     * like:user:userId -> int
     */
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE + SPLIT + userId;
    }
```

### 2.修改Service业务层（添加entityUserId属性，事务和查询获用户赞个数）

```java
  @Autowired
  private RedisTemplate redisTemplate;

  // 点赞 (记录谁点了哪个类型哪个留言/帖子id)
  public void like(int userId, int entityType, int entityId, int entityUserId){
      /**因为要用到两个redis操作，需使用事务**/
      redisTemplate.execute(new SessionCallback() {
          @Override
          public Object execute(RedisOperations redisOperations) throws DataAccessException {
              String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
              String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

              //判断like:entity:entityType:entityId 是否有对应的 userId
              Boolean isMember = redisOperations.opsForSet().isMember(entityLikeKey, userId);

              // 先查再开启事务
              redisOperations.multi();
              if (isMember) {
                  // 若已被点赞(即entityLikeKey里面有userId)则取消点赞->将userId从中移除
                  redisOperations.opsForSet().remove(entityLikeKey, userId);
                  // 该帖子的用户收到的点赞-1
                  redisOperations.opsForValue().decrement(userLikeKey);
              }else {
                  redisOperations.opsForSet().add(entityLikeKey, userId);
                  redisOperations.opsForValue().increment(userLikeKey);
              }

              return redisOperations.exec();
          }
      });
  }
```

```java
    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        // 注意这里Integet封装类型！！！！
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }
```

### 3.修改LikeController层（添加entityUserId属性）

```java
    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId){
        User user = hostHolder.getUser();
        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        // 获取对应帖子、留言的点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 获取当前登录用户点赞状态（1：已点赞 0：赞）
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        // 封装结果到Map
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        return CommunityUtil.getJSONString(0,null, map);
    }
```

### 4.同样在JS添加entityUserId属性

```javascript
function like(btn, entityType, entityId, entityUserId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");
            }else {
                alert(data.msg);
            }
        }
    );
}
```

### 5.编写个人主页UserController层

```java
    /**
     * 个人主页
     */
    @RequestMapping(value = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        // 进入某用户主页获取他(我)的点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        return "/site/profile";
    }
```

### 6.编写前端个人主页（核心部分）

```html
<span>获得了 <i th:text="${likeCount}">87</i> 个赞</span>

<a th:href="@{|/user/profile/${map.user.id}|}">
  点击头像进入某用户主页
</a>
```

# 关注功能（Redis+异步ajax）

## 关注、取消关注

### 1.编写工具类RedisKeyUtil统一关注Redis的key

关注：**k:v = followee:userId:entityType --> zset(entityId, date)**

粉丝：**k:v = follower:entityType:entityId -->zset(userId, date)**

```java
public class RedisKeyUtil {
    // 关注
    private static final String PREFIX_FOLLOWEE = "followee";
    // 粉丝
    private static final String PREFIX_FOLLOWER = "follower";
    /**
     * 某个用户关注的实体(用户，帖子)
     * followee:userId:entityType --> zset(entityId, date)
     */
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    /**
     * 某个实体拥有的粉丝
     * follower:entityType:entityId -->zset(userId, date)
     */
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT +entityType + SPLIT +entityId;
    }
}
```

### 2.编写Service层业务

```java
    @Autowired
    private RedisTemplate redisTemplate;

    /**关注**/
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                // 开启事务
                redisOperations.multi();
                /**
                 * System.currentTimeMillis()->用于获取当前系统时间,以毫秒为单位
                 * 关注时，首先将实体(用户或帖子)id添加用户关注的集合中，再将用户id添加进实体粉丝的集合中
                 */
                redisOperations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisOperations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return redisOperations.exec();
            }
        });
    }

    /**取消关注**/
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                // 开启事务
                redisOperations.multi();
                /**关注时，首先将实体(用户或帖子)id移除用户关注的集合中，再将用户id移除进实体粉丝的集合中**/
                redisOperations.opsForZSet().remove(followeeKey, entityId);
                redisOperations.opsForZSet().remove(followerKey, userId);

                return redisOperations.exec();
            }
        });
    }

    /**查询关注的实体(用户)数量**/
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        // opsForZSet().zCard获取有序集合中的数量
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**查询粉丝的实体数量**/
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /**查询当前用户是否已关注该实体**/
    // userId->当前登录用户  entityType->用户类型 entityId->关注的用户id
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey =RedisKeyUtil.getFolloweeKey(userId, entityType);
        /**
         * opsForZSet().score 获取有序集合中指定元素权重分数  followee:userId:entityType = entityId的分数（这里是时间）
         * 若有时间，则表明已关注；
         */
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }
```

### 3.编写Controller层

#### 3.1关注与取消关注按钮的实现（FollowController）

```java
    /**关注**/
    @RequestMapping(value = "/follow", method = RequestMethod.POST)
    @ResponseBody // 关注是异步请求
    public String follow(int entityType, int entityId) {
        followService.follow(hostHolder.getUser().getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0,"已关注");
    }

    /**取消关注**/
    @RequestMapping(value = "/unfollow", method = RequestMethod.POST)
    @ResponseBody // 关注是异步请求
    public String unfollow(int entityType, int entityId) {
        followService.unfollow(hostHolder.getUser().getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0,"已取消关注");
    }
```

#### 3.2主页中显示关注数量，粉丝数量（UserController）

```java
    /**
     * 个人主页
     */
    @RequestMapping(value = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);
        // 点赞数量
           ....
        // 关注数量(这里只考虑关注用户类型的情况)
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注 (必须是用户登录的情况)
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }
```

### 4.编写JS异步请求和前端页面（核心部分）

```javascript
$(function(){
  $(".follow-btn").click(follow);
});

function follow() {
  var btn = this;
  if($(btn).hasClass("btn-info")) {
    // 关注TA
    $.post(
      CONTEXT_PATH + "/follow",
      // "entityId":$(btn).prev().val() 获取btn按钮上一个的值
      {"entityType":3,"entityId":$(btn).prev().val()},
      function (data) {
        data = $.parseJSON(data);
        if(data.code == 0) {
          window.location.reload();
        } else {
          alert(data.msg);
        }});
  } else {
    // 取消关注
    $.post(
      CONTEXT_PATH + "/unfollow",
      {"entityType":3,"entityId":$(btn).prev().val()},
      function(data) {
        data = $.parseJSON(data);
        if(data.code == 0) {
          window.location.reload();
        } else {
          alert(data.msg);
        }});
  }}
```

```html
<input type="hidden" id="entityId" th:value="${user.id}">
              <!-- hasFollowed为true:已关注 ->按钮变灰 ，false:未关注 ->按钮变蓝  -->
<button type="button" th:class="|btn ${hasFollowed?'btn-secondary':'btn-info'} btn-sm float-right mr-5 follow-btn|"
              <!-- 只有登录过且当前登录用户不是看的自己的主页就显示已关注/关注TA -->
    th:text="${hasFollowed?'已关注':'关注TA'}" th:if="${loginUser!=null && loginUser.id!=user.id}">关注TA</button>
    
<span>关注了 <a th:text="${followeeCount}">5</a> 人</span>
<span>关注者 <a th:text="${followerCount}">123</a> 人</span>
```

## 关注列表（同粉丝列表）

### 1.编写Service层（查询某用户关注的人）

```java
    /**查询某用户关注的人**/
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        // 按最新时间倒序查询目标用户id封装在set<Integet>中
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }
        // 将user信息Map和redis用户关注时间Map一起封装到list
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId: targetIds) {
            HashMap<String, Object> map = new HashMap<>();
            // 用户信息map
            User user = userService.findUserById(targetId);
            map.put("user", user);
            // 目标用户关注时间map(将long型拆箱成基本数据类型)
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followeeTime", new Date(score.longValue()));

            list.add(map);
        }
        return list;
    }
```

### 2.编写Controller层

```java
  /** 查询某用户关注列表**/
  @RequestMapping(value = "/followees/{userId}", method = RequestMethod.GET)
  public String getFollowees(@PathVariable("userId")int userId, Page page, Model model) {
      // 当前访问的用户信息
      User user = userService.findUserById(userId);
      // Controller层统一处理异常
      if (user == null) {
          throw new RuntimeException("该用户不存在！");
      }
      model.addAttribute("user", user);
      // 设置分页信息
      page.setLimit(3);
      page.setPath("/followees/" + userId);
      page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

      List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
      if (userList != null) {
          for (Map<String, Object> map : userList) {
              User u = (User) map.get("user");
              map.put("hasFollowed", hasFollowed(u.getId()));
          }
      }
      model.addAttribute("users", userList);

      return "/site/followee";
  }
  
  /**判端当前登录用户与关注、粉丝列表的关注关系**/
  private Boolean hasFollowed(int userId) {
      if (hostHolder.getUser() == null) {
          return false;
      }
      // 调用当前用户是否已关注user实体Service
      return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
  }
```

### 3.编写前端页面

**3.1 带参数路径跳转**

```html
<span>关注了 <a th:href="@{|/followees/${user.id}|}" th:text="${followeeCount}">5</a> 人</span>
<span>关注者 <a th:href="@{|/followers/${user.id}|}" th:text="${followerCount}">123</a> 人</span>
```

**3.2  列表页面**
```html
  <li th:each="map:${users}">
    <a th:href="@{|/user/profile/{map.user.id}|}">
      <img th:src="${map.user.headerUrl}"alt="用户头像" >
    </a>
    <div>
      <h6>
        <span th:utext="${map.user.username}">落基山脉下的闲人</span>
        <span>
          关注于 <i th:text="${#dates.format(map.followerTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-28 14:13:25</i>
        </span>
      </h6>
      <div>
        <input type="hidden" id="entityId" th:value="${map.user.id}">
        <button type="button" th:class="|${map.hasFollowed?'btn-secondary':'btn-info'}|"
            th:text="${map.hasFollowed?'已关注':'关注TA'}" th:if="${loginUser!=null && loginUser.id!=map.user.id}">关注TA</button>
      </div>
    </div>
  </li>
````

# 系统通知功能（Kafka消息队列）

## 发送系统通知功能（点赞、关注、评论时通知）

### 1.编写Kafka消息队列事件Event实体类

```java
/**
 * Kafka消息队列事件（评论、点赞、关注事件
 */
public class Event {

    // Kafka必要的主题变量
    private String topic;
    // 发起事件的用户id
    private int userId;
    // 用户发起事件的实体类型（评论、点赞、关注类型）
    private int entityType;
    // 用户发起事件的实体(帖子、评论、用户)id
    private int entityId;
    // 被发起事件的用户id(被评论、被点赞、被关注用户)
    private int entityUserId;
    // 其他可扩充内容对应Comment中的content->显示用户xxx评论、点赞、关注了xxx
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    // 注意这里所有set方法返回Event类型,变成链式编程
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    // 方便外界直接调用key-value,而不用再封装一下传整个Map集合
    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
```

### 2.编写Kafka生产者

```java
/**
 * Kafka事件生产者（主动调用）相当于一个开关
 */
@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件
    public void fireMessage(Event event) {
        // 将事件发布到指定的主题,内容为event对象转化的json格式字符串
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
```

### 3.编写Kafka消费者

```java
/**
 * QQ:260602448--xumingyu
 * Kafka事件消费者(被动调用)
 * 对Message表扩充：1：系统通知，当生产者调用时，存入消息队列，消费者自动调用将event事件相关信息存入Message表
 */
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将record.value字符串格式转化为Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 注意：event中若data = null,是fastjson依赖版本的问题(不能太高1.0.xx)
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        // Message表中ToId设置为被发起事件的用户id
        message.setToId(event.getEntityUserId());
        // ConversationId设置为事件的主题（点赞、评论、关注）
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 设置content为可扩展内容，封装在Map集合中,用于显示xxx评论..了你的帖子
        HashMap<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityId", event.getEntityId());
        content.put("entityType", event.getEntityType());

        // 将event.getData里的k-v存到context这个Map中，再封装进message
        // Map.Entry是为了更方便的输出map键值对,Entry可以一次性获得key和value者两个值
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        // 将content(map类型)转化成字符串类型封装进message
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

    }
}
```

### 4.在CommunityConstant添加Kafka主题静态常量

```java
public interface CommunityConstant {
     /**
     * Kafka主题: 评论
     */
    String TOPIC_COMMENT = "comment";
    /**
     * Kafka主题: 点赞
     */
    String TOPIC_LIKE = "like";
    /**
     * Kafka主题: 关注
     */
    String TOPIC_FOLLOW = "follow";
    /**
     * 系统用户ID
     */
    int SYSTEM_USER_ID = 1;
}
```

### 5.处理触发评论事件CommentController

```java
    @RequestMapping(value = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);
        /**
         * 触发评论事件
         * 评论完后，调用Kafka生产者，发送系统通知
         */
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setEntityId(comment.getEntityId())
                .setEntityType(comment.getEntityType())
                .setUserId(hostHolder.getUser().getId())
                .setData("postId", discussPostId);
        /**
         * event.setEntityUserId要分情况设置被发起事件的用户id
         * 1.评论的是帖子，被发起事件（评论）的用户->该帖子发布人id
         * 2.评论的是用户的评论，被发起事件（评论）的用户->该评论发布人id
         */
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 先找评论表对应的帖子id,在根据帖子表id找到发帖人id
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());

        }
        eventProducer.fireMessage(event);
        
        return "redirect:/discuss/detail/" + discussPostId;
    }
```

### 6.处理触发关注事件FollowController

```java
    @RequestMapping(value = "/follow", method = RequestMethod.POST)
    @ResponseBody // 关注是异步请求
    public String follow(int entityType, int entityId) {
        followService.follow(hostHolder.getUser().getId(), entityType, entityId);
        /**
         * 触发关注事件
         * 关注完后，调用Kafka生产者，发送系统通知
         */
        Event event = new Event()
            .setTopic(TOPIC_FOLLOW)
            .setUserId(hostHolder.getUser().getId())
            .setEntityType(entityType)
            .setEntityId(entityId)
            .setEntityUserId(entityId);
        // 用户关注实体的id就是被关注的用户id->EntityId=EntityUserId
        eventProducer.fireMessage(event);
        
        return CommunityUtil.getJSONString(0, "已关注");
    }
```

### 7.处理触发点赞事件LikeController

```java
    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @ResponseBody
    // 加了一个postId变量，对应的前端和js需要修改
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        // 获取对应帖子、留言的点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 获取当前登录用户点赞状态（1：已点赞 0：赞）
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 封装结果到Map
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);
        /**
         * 触发点赞事件
         * 只有点赞完后，才会调用Kafka生产者，发送系统通知，取消点赞不会调用事件
         */
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setEntityId(entityId)
                    .setEntityType(entityType)
                    .setUserId(user.getId())
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            // 注意：data里面存postId是因为点击查看后链接到具体帖子的页面
            eventProducer.fireMessage(event);
        }
        return CommunityUtil.getJSONString(0, null, map);
    }
```

```html
<!--对应的前端postId变量以及js的修改-->
<a 
  th:onclick="|like(this,1,${post.id},${post.userId},${post.id});|">
</a>
function like(btn, entityType, entityId, entityUserId, postId) {
  $.post(
      CONTEXT_PATH + "/like",
      {"entityType": entityType, "entityId": entityId, "entityUserId": entityUserId, "postId":postId},
      function(data) {
      .....}
  );}
```

## 查询系统通知

### 1.编写Dao层接口(及Mapper.xml)

```java
/**
 * 查询某个主题最新通知
 */
Message selectLatestNotice(@Param("userId")int userId, @Param("topic")String topic);
/**
 * 查询某个主题通知个数
 */
int selectNoticeCount(@Param("userId")int userId, @Param("topic")String topic);
/**
 * 查询某个主题未读个数(topic可为null,若为null:查询所有类系统未读通知个数)
 */
int selectNoticeUnreadCount(@Param("userId")int userId, @Param("topic")String topic);
/**
* 分页查询某个主题的详情
*/
List<Message> selectNotices(@Param("userId")int userId, @Param("topic")String topic, @Param("offset")int offset, @Param("limit")int limit);

```

```sql
    <!--系统通知-->
    <select id="selectLatestNotice" resultType="Message">
        select <include refid="selectFields"></include>
        from message
        where id in (
          select max(id) from message
          where status != 2
          and from_id = 1
          and to_id = #{userId}
          and conversation_id = #{topic}
        )
    </select>

    <select id="selectNoticeCount" resultType="int">
        select count(id) from message
        where status != 2
        and from_id = 1
        and to_id = #{userId}
        and conversation_id = #{topic}
    </select>

    <!--topic为null时查询所有类系统未读通知--->
    <select id="selectNoticeUnreadCount" resultType="int">
        select count(id) from message
        where status = 0
        and from_id = 1
        and to_id = #{userId}
        <if test="topic!=null">
            and conversation_id = #{topic}
        </if>
    </select>

    <select id="selectNotices" resultType="Message">
        select <include refid="selectFields"></include>
        from message
        where status != 2
        and from_id = 1
        and to_id = #{userId}
        and conversation_id = #{topic}
        order by create_time desc
        limit #{offset}, #{limit}
    </select>
```

### 2.编写Service业务层

```java
    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }

    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }
```

### 3.编写MessageController层

#### 3.1查询系统通知接口（评论类通知、点赞类通知、关注类通知三种类似）

```java
    /**
     * 查询系统通知
     */
    @RequestMapping(value = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();
        /**查询评论类通知**/
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);

        if (message != null) {
            HashMap<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);

            // 转化message表中content为HashMap<k,v>类型
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            // 将content数据中的每一个字段都存入map
            // 用于显示->用户[user] (评论、点赞、关注[entityType])...了你的(帖子、回复、用户[entityId]) 查看详情连接[postId]
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            // 共几条会话
            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count);
            // 评论类未读数
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("commentNotice", messageVO);
        }

        /**查询点赞类通知**/
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

        if (message != null) {
            HashMap<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);
            // 转化message表中content为HashMap<k,v>类型
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            // 将content数据中的每一个字段都存入map
            // 用于显示->用户[user] (评论、点赞、关注[entityType])...了你的(帖子、回复、用户[entityId]) 查看详情连接[postId]
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            // 共几条会话
            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);
            // 点赞类未读数
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unreadCount", unreadCount);
            model.addAttribute("likeNotice", messageVO);
        }

        /**查询关注类通知**/
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);

        if (message != null) {
            HashMap<String, Object> messageVO = new HashMap<>();
            messageVO.put("message", message);
            // 转化message表中content为HashMap<k,v>类型
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            // 将content数据中的每一个字段都存入map
            // 用于显示->用户[user] (评论、点赞、关注)...了你的(帖子、回复、用户[entityType]) 查看详情连接[postId]
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            // 共几条会话
            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);
            // 关注类未读数
            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unreadCount", unreadCount);
            model.addAttribute("followNotice", messageVO);
        }

        // 查询未读私信数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        // 查询所有未读系统通知数量
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }
```

#### 3.2查询系统通知详情页接口

```java
    /**
     * 查询系统通知详情页（分页）
     */
    @RequestMapping(value = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic")String topic, Page page, Model model) {
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        // 聚合拼接User
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                HashMap<String, Object> map = new HashMap<>();
                // 将查询出来的每一个通知封装Map
                map.put("notice", notice);
                // 发起事件的user
                map.put("user", userService.findUserById(user.getId()));

                // 把message中的content内容转化Object
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 系统通知->id=1的系统用户
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        //设置已读(当打开这个页面是就更改status =1)
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
```

### 4.通过AOP编程实现查询未读消息总数(私信消息+系统消息)

#### 4.1编写MessageInterceptor拦截器

```java
@Component
public class MessageInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private MessageService messageService;
    // 查询未读消息总数(AOP),controller之后，渲染模板之前
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);

            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
        }
}}

// index页前端对应代码
<li th:if="${loginUser!=null}">
  <a th:href="@{/letter/list}">消息
    <span th:text="${allUnreadCount!=0?allUnreadCount:''}">消息未读总数</span>
  </a>
</li>
```

#### 4.2注册拦截器

```java
  @Autowired
  private MessageInterceptor messageInterceptor;
  public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(messageInterceptor)
          .excludePathPatterns("/* */*.css", "/**/ *.js", "/* */*.png", "/ **/ *.jpg", "/* */*.jpeg");
  }
```

### 5.编写前端页面（核心部分）

#### 5.1系统通知页

```html
<li>
  <a class="active" th:href="@{/notice/list}">
    系统通知<span th:text="${noticeUnreadCount}" th:if="${noticeUnreadCount!=0}">系统通知未读数</span>
  </a>
</li>

<!-- 通知列表 -->
<ul class="list-unstyled">
   <!--评论类通知-->
   <li th:if="${commentNotice!=null}">
     <span th:text="${commentNotice.unreadCount!=0?commentNotice.unreadCount:''}">评论通知未读数</span>
     <img src="http://xxx.png" alt="通知图标">
      <h6>
        <span>评论</span>
        <span th:text="${#dates.format(commentNotice.message.createTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-28 14:13:25</span>
      </h6>
      <div>
        <a th:href="@{/notice/detail/comment}">
          用户 <i th:utext="${commentNotice.user.username}">评论发起人用户名</i>
          评论了你的<b th:text="${commentNotice.entityType==1?'帖子':'回复'}">帖子</b> ...</a>
        <ul>
          <span>共 <i th:text="${commentNotice.count}">3</i> 条会话</span>
        </ul>
      </div>
   </li>
   <!--点赞类通知-->
   <li th:if="${likeNotice!=null}">
        <span th:text="${likeNotice.unreadCount!=0?likeNotice.unreadCount:''}">3</span>
        <img src="http://like.png" alt="通知图标">
        <div>
          <h6>
            <span>赞</span>
            <span th:text="${#dates.format(likeNotice.message.createTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-28 14:13:25</span>
          </h6>
          <div>
            <a th:href="@{/notice/detail/like}">
              用户
              <i th:utext="${likeNotice.user.username}">nowcoder</i>
              点赞了你的<b th:text="${likeNotice.entityType==1?'帖子':'回复'}">帖子</b> ...
            </a>
            <ul>
              <span class="text-primary">共 <i th:text="${likeNotice.count}">3</i> 条会话</span>
            </ul>
          </div>
        </div>
      </li>
   <!--关注类通知-->
   <li th:if="${followNotice!=null}">
        <span th:text="${followNotice.unreadCount!=0?followNotice.unreadCount:''}">3</span>
        <img src="http://follow.png" class="mr-4 user-header" alt="通知图标">
        <div>
          <h6>
            <span>关注</span>
            <span th:text="${#dates.format(followNotice.message.createTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-28 14:13:25</span>
          </h6>
          <div>
            <a th:href="@{/notice/detail/follow}">
              用户
              <i th:utext="${followNotice.user.username}">nowcoder</i>
              关注了你 ...
            </a>
            <ul>
              <span class="text-primary">共 <i th:text="${followNotice.count}">3</i> 条会话</span>
            </ul>
          </div>
        </div>
      </li>
</ul>
```

#### 5.2系统通知详情页

```html
<div class="col-4 text-right">
  <button type="button" class="btn btn-secondary btn-sm" onclick="back();">返回</button>
</div>

<!-- 通知列表 -->
<ul>
  <li th:each="map:${notices}">
    <img th:src="${map.fromUser.headerUrl}" alt="系统图标">
    <div>
      <div>
        <strong th:utext="${map.fromUser.username}">系统名</strong>
        <small th:text="${#dates.format(map.notice.createTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-25 15:49:32</small>
      </div>
      <div>
        <!--显示评论信息-->
        <span th:if="${topic.equals('comment')}">
          用户
          <i th:utext="${map.user.username}">发起事件人</i>
          评论了你的<b th:text="${map.entityType==1?'帖子':'回复'}">帖子</b>,
          <a th:href="@{|/discuss/detail/${map.postId}|}">点击查看</a> !
        </span>
        <!--显示点赞信息-->
        <span th:if="${topic.equals('like')}">
          用户
          <i th:utext="${map.user.username}">发起事件人</i>
          点赞了你的<b th:text="${map.entityType==1?'帖子':'回复'}">帖子</b>,
          <a th:href="@{|/discuss/detail/${map.postId}|}">点击查看</a> !
        </span>
        <!--显示关注信息-->
        <span th:if="${topic.equals('follow')}">
          用户
          <i th:utext="${map.user.username}">发起事件人</i>
          关注了你,
          <a th:href="@{|/user/profile/${map.user.id}|}">点击查看</a> !
        </span>
      </div>
    </div>
  </li>
</ul>
<script>
  function back() {
    location.href = CONTEXT_PATH + "/notice/list";
  }
</script>

```

# 搜索功能（Elasticsearch+Kafka）

## 1.编写实体类映射到Elasticsearch服务器

```java
// Elasticsearch表名
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscussPost {
    @Id
    private int id;

    // Elaticsearch与数据库表映射
    @Field(type = FieldType.Integer)
    private int userId;

    // analyzer：最大中文分词解析器, searchAnalyzer：智能中文分词解析器
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Double)
    private double score;
```

## 2.编写xxxRepository接口继承ElasticsearchRepository\<Class, Integer>

```java
/**
 * ElasticsearchRepository<DiscussPost, Integer>
 * DiscussPost：接口要处理的实体类
 * Integer：实体类中的主键是什么类型
 * ElasticsearchRepository：父接口，其中已经事先定义好了对es服务器访问的增删改查各种方法。Spring会给它自动做一个实现，我们直接去调就可以了。
 */
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
}
```

## 3.编写ElasticsearchService业务层

```java
/**
 * 用Elasticsearch服务器搜索帖子service
 */
@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

    /**
     * Elasticsearch高亮搜索
     * current：当前页（不是offset起始页）
     */
    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // new SearchResultMapper()匿名类，处理高亮
        return elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    // elasticsearch中将json格式数据封装为了map,在将map字段存进post中
                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    // createTime字符串是Long类型
                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        // [0]->搜寻结果为多段时，取第一段
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable,
                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
            }
        });
    }
}
```

## 4.修改发布帖子和增加评论Controller

发布帖子时，将帖子异步提交到Elasticsearch服务器

增加评论时，将帖子异步提交到Elasticsearch服务器

```java
     /**
      * Kafka主题: 发布帖子(常量接口)
      */
    String TOPIC_PUBILISH = "publish";
    
    /**--------------------------------------------------------**/
    @RequestMapping(value = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
      // ............
      
     /**
      * 增加评论时，将帖子异步提交到Elasticsearch服务器
      * 通过Kafka消息队列去提交，修改Elasticsearch中帖子的评论数
      */
      //若评论为帖子类型时，才需要加入消息队列处理
      if (comment.getEntityType() == ENTITY_TYPE_POST) {
          event = new Event()
                  .setTopic(TOPIC_PUBILISH)
                  .setUserId(comment.getUserId())
                  .setEntityType(ENTITY_TYPE_POST)
                  .setEntityId(discussPostId);
          eventProducer.fireMessage(event);
      }
     return "redirect:/discuss/detail/" + discussPostId;
    }
```

```java
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    // 异步请求要加@ResponseBody,且不要在Controller层用Model
    public String addDiscussPost(String title, String content) {
    //.................
    
    /**
     * 发布帖子时，将帖子异步提交到Elasticsearch服务器
     * 通过Kafka消息队列去提交，将新发布的帖子存入Elasticsearch
     */
    Event event = new Event()
            .setTopic(TOPIC_PUBILISH)
            .setUserId(user.getId())
            .setEntityType(ENTITY_TYPE_POST)
            .setEntityId(post.getId());
    eventProducer.fireMessage(event);

    // 返回Json格式字符串,报错的情况将来统一处理
    return CommunityUtil.getJSONString(0, "发布成功！");
    }
```

## 5.在消费组件中增加方法（消费帖子发布事件）

```java
    /**
     * 消费帖子发布事件，将新增的帖子和添加评论后帖子评论数通过消息队列的方式save进Elastisearch服务器中
     */
    @KafkaListener(topics = {TOPIC_PUBILISH})
    public void handleDiscussPostMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将record.value字符串格式转化为Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 注意：event若data=null,是fastjson依赖版本的问题
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }
```

## 6.编写SearchController类

```java
@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    // search?keyword=xxx
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        // 搜索帖子
        // 在调用elasticsearchService完成搜索的时候，查询条件设置的是从第几页开始，所以要填getCurrent，填getOffset会导致翻页的时候查询错误
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();

        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.findUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        // 为了页面上取的默认值方便
        model.addAttribute("keyword", keyword);

        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 :(int) searchResult.getTotalElements());
        return "/site/search";
    }
}
```

## 7.编写前端页面（核心部分）

```html
  <!-- 搜索表单 -->
  <form method="get" th:action="@{/search}">
    <!--th:value->设置默认值  model.addAttribute("keyword", keyword) -->
    <input type="search" aria-label="Search" name="keyword" th:value="${keyword}"/>
    <button type="submit">搜索</button>
  </form>
```

```html
<li th:each="map:${discussPosts}">
  <img th:src="${map.user.headerUrl}" alt="用户头像" style="width:50px;height:50px">
  <div>
    <a th:href="@{|/discuss/detail/${map.post.id}|}" th:utext="${map.post.title}"><em>搜索词高亮显示</em>可链接的帖子标题</a>
    <div th:utext="${map.post.content}">
       帖子内容<em>搜索词高亮显示</em>
    </div>
    <div>
      <u th:utext="${map.user.username}">寒江雪</u>
      发布于 <b th:text="${#dates.format(map.post.createTime,'yyyy-MM-dd HH:mm:ss')}">帖子发布时间</b>
      <ul>
        <li>赞 <i th:text="${map.likeCount}">11</i></li>
        <li>|</li>
        <li>回复 <i th:text="${map.post.commentCount}">7</i></li>
      </ul>
    </div>
  </div>
</li>
```

# 权限控制

## 部署SpringSecurity权限控制

### 1.配置SecurityConfig类

**登录检查：废弃之前的拦截器配置，采用SpringSecurity**

**权限配置：对所有请求分配访问权限**

```java
/**
 * springsecurity配置
 * 之所以没有configure(AuthenticationManagerBuilder auth)，是因为要绕过security自带的方案
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 忽略静态资源
        web.ignoring().antMatchers("/resources/* *");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                // 需要授权的请求
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/* *",
                        "/letter/* *",
                        "/notice/* *",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                // 这3中权限可以访问以上请求
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                // 其他请求方行
                .anyRequest().permitAll()
                // 禁用 防止csrf攻击功能
                .and().csrf().disable();


        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 没有登录
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        // 同步请求重定向返回HTML，异步请求返回json
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            // 处理异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦!"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                // 权限不足
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        //底层：private String logoutUrl = "/logout";
        http.logout().logoutUrl("/securitylogout");
    }
}
```

### 2.编写UserService增加自定义登录认证方法绕过security自带认证流程

```java
    /**绕过Security认证流程，采用原来的认证方案,封装认证结果**/
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
```

### 3.编写登录凭证拦截器LoginTicketInterceptor

构建用户认证结果,并存入SecurityContext,以便于Security进行授权

```java
    @Override
    /**在Controller访问所有路径之前获取凭证**/
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      //...................................
      
      if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
        // ...............................
        /**
         * 构建用户认证结果,并存入SecurityContext,以便于Security进行授权
         */
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, user.getPassword(), userService.getAuthorities(user.getId()));
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
      }
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 释放线程资源
        hostHolder.clear();
        // 释放SecurityContext资源
        SecurityContextHolder.clearContext();
    }
```

### 4.退出登录时释放SecurityContext资源

```java
    /**
     * 退出登录功能
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        // 释放SecurityContext资源
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
```

### 5.注意：防止CSRF攻击

CSRF攻击原理

![](image/防止CSRF攻击_rVHfT_BFS2.PNG)

由于服务端SpringSecurity自带防止CSRF攻击，因此只要编写前端页面防止CSRF攻击即可 \ （常发生在提交表单时）

```html
  <!--访问该页面时,在此处生成CSRF令牌.-->
  <meta name="_csrf" th:content="${_csrf.token}">
  <meta name="_csrf_header" th:content="${_csrf.headerName}">
```

**Ajax异步请求时携带该参数**

```javascript
function publish() {
   $("#publishModal").modal("hide");
   // 发送AJAX请求之前,将CSRF令牌设置到请求的消息头中.
   var token = $("meta[name='_csrf']").attr("content");
   var header = $("meta[name='_csrf_header']").attr("content");
   $(document).ajaxSend(function(e, xhr, options){
       xhr.setRequestHeader(header, token);
   });
   // ...............................
}
```

## 置顶、加精、删除

### 1.编写Mapper、Service层

思路：改变帖子状态

置顶：type = (0-正常，1-置顶）   加精：status = (0-正常，1-加精，2-删除)

```sql
    int updateType(@Param("id")int id,@Param("type") int type);
    int updateStatus(@Param("id")int id,@Param("status") int status);
    
    <!--------------------Mapper.xml------------------------->
    <update id="updateType">
        update discuss_post set type = #{type} where id = #{id}
    </update>
    <update id="updateStatus">
        update discuss_post set status = #{status} where id = #{id}
    </update>
    
    <!--------------------Service层------------------------->
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }
```

### 2.编写DiscussPostController层

```java
    // 置顶、取消置顶(与以下类似)
    @RequestMapping(value = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        // 获取置顶状态，1为置顶，0为正常状态,1^1=0 0^1=1
        int type = post.getType() ^ 1;
        discussPostService.updateType(id, type);
        // 返回结果给JS异步请求
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", type);

        // 触发事件，修改Elasticsearch中的帖子type
        Event event = new Event()
                .setTopic(TOPIC_PUBILISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireMessage(event);

        return CommunityUtil.getJSONString(0, null, map);
    }

    // 加精、取消加精
    @RequestMapping(value = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        int status = post.getStatus() ^ 1;
        discussPostService.updateStatus(id, status);
        // 返回结果给JS异步请求
        HashMap<String, Object> map = new HashMap<>();
        map.put("status", status);

        // 触发事件，修改Elasticsearch中的帖子status
        Event event = new Event()
                .setTopic(TOPIC_PUBILISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireMessage(event);

        return CommunityUtil.getJSONString(0, null, map);
    }

    // 删除
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);

        // 触发删帖事件,将帖子从Elasticsearch中删除
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireMessage(event);

        return CommunityUtil.getJSONString(0);
    }
```

### 3.编写Kafka消费者中删除（TOPIC\_DELETE）的主题事件

```java
    /**帖子删除事件**/
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将record.value字符串格式转化为Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 注意：event若data=null,是fastjson依赖版本的问题
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
```

### 4.在SecurityConfig中给予（置顶、加精、删除）权限

```java
  // 授权
  http.authorizeRequests()
          // 需要授权的请求
          // ...............
          )
          .antMatchers(
                  "/discuss/top",
                  "/discuss/wonderful"
          )
          .hasAnyAuthority(
                  AUTHORITY_MODERATOR // 版主授予加精、置顶权限
          )
          .antMatchers(
                  "/discuss/delete"
          )
          .hasAnyAuthority(
                  AUTHORITY_ADMIN // 管理员授予删除帖子权限
          )
          // 其他请求方行
          .anyRequest().permitAll()
          // 禁用 防止csrf攻击功能
          .and().csrf().disable();
```

### 5.编写前端代码（核心部分）

#### 5.1引用pom.xml，使用sec:xxx

```xml
  <dependency>
      <groupId>org.thymeleaf.extras</groupId>
      <artifactId>thymeleaf-extras-springsecurity5</artifactId>
  </dependency>
```

#### 5.2 引入thymeleaf支持security的头文件

```html
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

```html
<div>
  <input type="hidden" id="postId" th:value="${post.id}">
  <button type="button" class="btn" id="topBtn"
      th:text="${post.type==1?'取消置顶':'置顶'}" sec:authorize="hasAnyAuthority('moderator')">置顶</button>
  <button type="button" class="btn" id="wonderfulBtn"
      th:text="${post.status==1?'取消加精':'加精'}" sec:authorize="hasAnyAuthority('moderator')">加精</button>
  <button type="button" class="btn" id="deleteBtn"
      th:disabled="${post.status==2}" sec:authorize="hasAnyAuthority('admin')">删除</button>
</div>
```

#### 5.3 编写JS中的异步Ajax请求

```javascript
// 页面加载完以后调用
$(function(){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});

// 置顶、取消置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $("#topBtn").text(data.type == 1 ? '取消置顶':'置顶');
            } else {
                alert(data.msg);
            }
        }
    );
}

// 加精、取消加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $("#wonderfulBtn").text(data.status == 1 ? '取消加精':'加精');
            } else {
                alert(data.msg);
            }
        }
    );
}

// 删除
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    );
}
```

# 网站数据统计（Redis：HyperLogLog、BitMap）

## 1.编写RedisUtil规范Key值

```java
    // UV (网站访问用户数量---根据Ip地址统计(包括没有登录的用户))
    private static final String PREFIX_UV = "uv";
    // DAU (活跃用户数量---根据userId)
    private static final String PREFIX_DAU = "dau";
    
    /**
     * 存储单日ip访问数量（uv）--HyperLogLog ---k:时间 v:ip  (HyperLogLog)
     * 示例：uv:20220526 = ip1,ip2,ip3,...
     */
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    /**
     * 获取区间ip访问数量（uv）
     * 示例：uv:20220525:20220526 = ip1,ip2,ip3,...
     */
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    /**
     * 存储单日活跃用户（dau）--BitMap ---k:date v:userId索引下为true  (BitMap)
     * 示例：dau:20220526 = userId1索引--(true),userId2索引--(true),....
     */
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    /**
     * 获取区间活跃用户
     * 示例：dau:20220526:20220526
     */
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }
```

## 2.编写DataService业务层

```java
    @Autowired
    private RedisTemplate redisTemplate;

    // 将Date类型转化为String类型
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
    
    /*********************** HypeLogLog*************************/
    // 将指定ip计入UV---k:当前时间 v:ip
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    // 统计指定日期范围内的ip访问数UV
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (start.after(end)) {
            throw new IllegalArgumentException("请输入正确的时间段！");
        }
        // 整理该日期范围内的Key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            // 获取该日期范围内的每一天的Key存入集合
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            // 日期+1(按照日历格式)
            calendar.add(Calendar.DATE, 1);
        }
        // 合并日期范围内相同的ip
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        // 获取keyList中的每一列key进行合并
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());

        // 返回统计结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    /*********************** BitMap *****************************/
    // 将指定用户计入DAU --k:当前时间 v:userId
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    // 统计指定日期范围内的DAU日活跃用户
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (start.before(end)) {
            throw new IllegalArgumentException("请输入正确的时间段！");
        }
        // 整理该日期范围内的Key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            // 日期+1(按照日历格式)
            calendar.add(Calendar.DATE, 1);
        }

        // 进行OR运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));

                connection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });}
```

## 3.在DataInterceptor拦截器中调用Service(每次请求最开始调用)

```java
@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;
    @Autowired
    private HostHolder hostHolder;
    // 在所有请求之前存用户访问数和日活跃人数
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求用户的ip地址，统计UV
        String ip = request.getRemoteHost();
        dataService.recordUV(ip);

        // 统计DAU
        User user = hostHolder.getUser();
        if (user != null) {
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
/*****************************注册拦截器*********************************/
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private DataInterceptor dataInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dataInterceptor)
          .excludePathPatterns("/* */*.css", "/ **/ *.js", "/* */*.png", "/**/ *.jpg", "/* */*.jpeg");
    }
}
```

## 4.编写DataController用以渲染模板

```java
    /**
     * 统计页面
     */
    @RequestMapping(value = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }
    /**
     * 统计网站UV(ip访问数量)
     * @DateTimeFormat将时间参数转化为字符串
     */
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        // 转发到 /data请求
        return "forward:/data";
    }
    /**
     * 统计网站DAU(登录用户访问数量)
     */
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data";
    }
```

## 5.编写SecurityConfig进行权限控制

```java
    .antMatchers(
            "/discuss/delete",
            "/data/* *"
    )
    .hasAnyAuthority(
            AUTHORITY_ADMIN
    )
```

## 6.编写前端管理员专用页面（核心部分）

```html
  <!-- 网站UV (活跃用户类似)--> 
  <div>
      <h6> 网站 访问人数</h6>
      <form method="post" th:action="@{/data/uv}">
          <input name="start" th:value="${#dates.format(uvStartDate,'yyyy-MM-dd')}" type="date"/>
          <input name="end" th:value="${#dates.format(uvEndDate,'yyyy-MM-dd')}" type="date"/>
          <button type="submit">开始统计</button>
      </form>
      <li>
          统计结果
          <span th:text="${uvResult}">访问人数</span>
      </li> 
  </div>
```

# 热帖排行（Quartz线程池、Redis）

## 1.编写RedisUtil规范Key值

```java
    // 热帖分数 (把需要更新的帖子id存入Redis当作缓存)
    private static final String PREFIX_POST = "post";
    
    /**
     *  帖子分数 (发布、点赞、加精、评论时放入)
     */
    public static String getPostScore() {
        return PREFIX_POST + SPLIT + "score";
    }
```

## 2.处理发布、点赞、加精、评论时计算分数，将帖子id存入Key

### 2.1发布帖子时初始化分数

```java
      /**
       * 计算帖子分数
       * 将新发布的帖子id存入set去重的redis集合------addDiscussPost()
       */
      String redisKey = RedisKeyUtil.getPostScore();
      redisTemplate.opsForSet().add(redisKey, post.getId());
```

### 2.2点赞时计算帖子分数

```java
      /**
       * 计算帖子分数
       * 将点赞过的帖子id存入set去重的redis集合------like()
       */
      if (entityType == ENTITY_TYPE_POST) {
          String redisKey = RedisKeyUtil.getPostScore();
          redisTemplate.opsForSet().add(redisKey, postId);
      }
```

### 2.3评论时计算帖子分数

```java
      if (comment.getEntityType() == ENTITY_TYPE_POST) {
          /**
          * 计算帖子分数
          * 将评论过的帖子id存入set去重的redis集合------addComment()
          */
          String redisKey = RedisKeyUtil.getPostScore();
          redisTemplate.opsForSet().add(redisKey, discussPostId);
      }
```

### 2.4加精时计算帖子分数

```java
      /**
       * 计算帖子分数
       * 将加精的帖子id存入set去重的redis集合-------setWonderful()
       */
      String redisKey = RedisKeyUtil.getPostScore();
      redisTemplate.opsForSet().add(redisKey, id);
```

## 3.定义Quartz热帖排行Job

```java
/**热帖排行定时刷新任务**/
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    // 网站创建时间
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-10-22 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化时间失败!", e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScore();
        // 处理每一个key
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数" + operations.size());
        while (operations.size() > 0) {
            // 刷新每一个从set集合里弹出的postId
            this.refresh((Integer)operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕！");
    }
    // 从redis中取出每一个value:postId
    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            logger.error("该帖子不存在：id = " + postId);
            return;
        }
        if(post.getStatus() == 2){
            logger.error("帖子已被删除");
            return;
        }

        /**
         * 帖子分数计算公式：[加精（75）+ 评论数*  10 + 点赞数*  2] + 距离天数
         */
        // 是否加精帖子
        boolean wonderful = post.getStatus() == 1;
        // 点赞数量
        long liketCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        // 评论数量
        int commentCount = post.getCommentCount();

        // 计算权重
        double weight = (wonderful ? 75 : 0) + commentCount*  10 + liketCount*  2;
        // 分数 = 取对数(帖子权重) + 距离天数
        double score = Math.log10(Math.max(weight, 1)) + (post.getCreateTime().getTime() - epoch.getTime()) / (1000*  3600* 24);

        // 更新帖子分数
        discussPostService.updateScore(postId, score);
        // 同步搜索数据
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
```

## 4.配置Quartz的PostScoreRefreshJob

```java
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }
    @Bean
    public SimpleTriggerFactoryBean PostScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(3000);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
```

## 5.修改主页帖子显示(Mapper、Service、Controller)

### 5.1 Mapper

```java
    // orderMode=0：最新  orderMode=1：最热
    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit,@Param("orderMode")int orderMode);

```

```sql
    <select id="selectDiscussPosts" resultType="DiscussPost">
        select
        <include refid="selectFields"></include>
        from discuss_post
        where status!=2
        <if test="userId!=0">
            and user_id=#{userId}
        </if>
        <if test="orderMode==0">
            order by type desc,create_time desc
        </if>
        <if test="orderMode==1">
            order by type desc,score desc,create_time desc
        </if>
        limit #{offset},#{limit}
    </select>
```

### 5.2 Service

```java
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }
```

### 5.3 Controller

```java
    @RequestMapping(value = "/index", method = RequestMethod.GET)
    // @RequestParam(name = "orderMode") 这是从前端传参数方法是：/index?xx 与Controller绑定
    public String getIndexPage(Model model, Page page,@RequestParam(name = "orderMode",defaultValue = "0") int orderMode) {
        
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPost = new ArrayList<>();

        if (list!=null){
            for(DiscussPost post:list) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPost.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPost);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }
```

## 6编写前端页面实现切换最新/最热帖子显示

```html
  <!-- 切换最新/最热帖子 -->
  <li class="nav-item">
    <a th:class="|nav-link ${orderMode==0?'active':''}|" th:href="@{/index(orderMode=0)}">最新</a>
  </li>
  <li class="nav-item">
    <a th:class="|nav-link ${orderMode==1?'active':''}|" th:href="@{/index(orderMode=1)}">最热</a>
  </li>
```

# 文件上传至云服务器(七牛云服务器)

## 绑定云服务器

### 1.引入pom.xml

```xml
    <!--七牛云服务器-->
    <dependency>
        <groupId>com.qiniu</groupId>
        <artifactId>qiniu-java-sdk</artifactId>
        <version>7.2.28</version>
    </dependency>
```

### 2.配置yml文件（服务器参数）

```yaml
# qiniu
qiniu:
  # 七牛云密钥(个人设置->密钥管理)
  key:
    access: 7Ia7E86E3B9XTQ9TrlA5l_E-_WBnkmXQhxoE3-_n
    secret: 17Ab9TcKnyn_jw4-a0XyH6iD_acl0KaKGEi6_Hqc
  bucket:
    # 头像上传云服务器配置(七牛云对象存储)
    header:
      name: xmyheader
      url: http://rcmsg2hwa.hb-bkt.clouddn.com
    # 分享功能云服务器配置
    share:
      name: xmyshare
      url: http://rcmscfkkw.hb-bkt.clouddn.com
```

## 将头像上传至云服务器

### 客户端上传：

—将客户端数据提交给云服务器，并等待其响应

—用户上传头像时，将表单数据提交给服务器

### 1.修改文件上传相应的Controller(这里是UserController)

```java
    @LoginRequired//自定义注解
    @RequestMapping(value = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        /**设置页面加载时就开始配置云服务器信息**/
        // 上传随机文件名称
        String fileName = CommunityUtil.generateUUID();
        // 设置返回给云服务器的响应信息（规定用StringMap）
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传云服务器的凭证
        Auth auth = Auth.create(accessKey, secretKey);
        // 上传指定文件名到云服务器指定空间，传入密钥，过期时间
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        // 七牛云规定：表单需要携带的参数
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    /**
     * 异步更新头像路径（云服务器异步返回Json,而不是返回页面，不然乱套）
     */
    @RequestMapping(value = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;
        // 将数据库头像url更换成云服务器图片url
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }
```

### 2.编写更新头像路径时js异步ajax

```javascript
// 上传到七牛云服务器的异步处理方法
$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    // 表单异步提交文件不能用$.post--不能映射文件类型，所以用原生$.ajax
    $.ajax({
        // 七牛云华北地区上传地址
        url: "http://upload-z1.qiniup.com",
        method: "post",
        // 不要把表单内容转为字符串（因为是上传图片文件）
        processData: false,
        // 不让JQuery设置上传类型(使用浏览器默认处理方法将二进制文件随机加边界字符串)
        contentType: false,
        // 传文件时需要这样传data
        data: new FormData($("#uploadForm")[0]),
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    // <form>表单没写action，就必须返回false
    return false;
}
```

## 将分享图片上传至云服务器

### 服务器直传：

**—本地应用服务器将数据直接提交给云服务器，并等待其响应**

**—分享时，服务端将自动生成的图片，直接提交给云服务器**

### 1.编写生成长图到本地Controller(使用消息队列处理并发)

```java
/**
 * wkhtmltopdf实现生成分享长图功能
 */
@Controller
public class ShareController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        // 文件名
        String fileName = CommunityUtil.generateUUID();

        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireMessage(event);

        // 返回访问路径
        Map<String, Object> map = new HashMap<>();
        //map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
        map.put("shareUrl", shareBucketUrl + "/" + fileName);

        return CommunityUtil.getJSONString(0, null, map);
    }
}

```

### 2.编写Kafka消费者—上传到云服务器

```java
    /**执行wk命令行的位置**/
    @Value("${wk.image.command}")
    private String wkImageCommand;

    /**存储wk图片位置**/
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    /**
     * 使用云服务器获取长图
     */
    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    /**定时器避免还没生成图片就上传服务器**/
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    
    /**
     * 消费wkhtmltopdf分享事件
     */
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // 执行cmd d:/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com d:/wkhtmltopdf/wk-images/2.png命令
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);
    }

    class UploadTask implements Runnable {
        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 生成失败
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长,终止任务:" + fileName);
                future.cancel(true);
                return;
            }
            // 上传失败
            if (uploadTimes >= 3) {
                logger.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true);
                return;
            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                // 指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));
                try {
                    // 开始上传图片
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    // 处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }
```

# 使用Caffine本地缓存优化网站性能(缓存主页热门帖子)

## 1.缓存概念

![](image/1_smDsDxDSq8.PNG)

注意：**本地缓存一般不缓存与用户相关的数据（如：登录凭证）原因如下图**

![](image/缓存_dFCbkzZUe-.PNG)

注意：**二级缓存流程如下图所示**

![](image/二级缓存_pg01-CvUun.PNG)

## 2.引入caffine依赖项

```xml
        <!--caffeine本地缓存优化热门帖子-->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
            <version>2.9.3</version>
        </dependency>
```

## 3.编写yml配置caffine全局变量

```yaml
# caffeine本地缓存优化热门帖子
caffeine:
  posts:
    # 最大缓存15页
    max-size: 15
    expire-seconds: 180
```

## 4.修改DiscussPostService业务层分页查询方法

```java
    /**
     * 使用caffine缓存热门帖子(可用Jmeter压力测试)
     * QQ:260602448
     * Caffeine核心接口: Cache, LoadingCache(常用同步), AsyncLoadingCache(异步)
     */
    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;
    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;
    
    // 项目启动时初始化缓存
    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    // load方法：当没有缓存时，查询数据库
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 这里可用二级缓存：Redis -> mysql
                        logger.debug("正在从数据库中加载热门帖子！");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("正在从数据库加载热门帖子总数！");
                        return discussPostMapper.selectDiscussRows(key);
                    }
                });
    }

    /**
     * 主页分页查询帖子（使用缓存查询热门帖子->即userId=0,orderMode=1）
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if (userId == 0 && orderMode ==1) {
            logger.debug("正在从Caffeine缓存中加载热门帖子！");
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("正在从数据库中加载热门帖子！");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        // userId=0：查询所有帖子
        if (userId == 0) {
            logger.debug("正在从Caffeine缓存中加载热门帖子！");
            return postRowsCache.get(userId);
        }
        logger.debug("正在从数据库加载热门帖子总数！");
        return discussPostMapper.selectDiscussRows(userId);
    }
```

# 统一处理异常

![](image/1_I_gGzxILnp.PNG)

## 1.将error/404.html或500.html放在templates

**注意：springboot默认在templates资源路径下面新建error目录，添加404.html和500.html页面就会自动配置上错误页面自动跳转**

## 2.定义一个控制器通知组件，处理所有Controller所发生的异常

```java
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage());
        // 循环打印异常栈中的每一条错误信息并记录
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        // 判断异常返回的是HTML还是Json异步格式字符串
        String xRequestedWith = request.getHeader("x-requested-with");
        // XMLHttpRequest: Json格式字符串
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            // 页面响应普通plain字符串格式
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
```

```java
@RequestMapping(value = "error", method = RequestMethod.GET)
public String getErrorPage(){
    return "/error/500";
}
```

# 统一记录日志

## 1.AOP概念（面向切面编程）

&#x20;常见的使用场景有：**权限检查、记录日志、事务管理**

&#x20;Joinpoint：**目标对象上织入代码的位置叫做joinpoint**

&#x20;Pointcut：是用来定义当前的横切逻辑准备织入到哪些连接点上 （如service所有方法）

&#x20;Advice：**用来定义横切逻辑，即在连接点上准备织入什么样的逻辑**

&#x20;Aspect：**是一个用来封装切点和通知的组件**

&#x20;织入：**就是将方面组件中定义的横切逻辑，织入到目标对象的连接点的过程**

![](image/4_xPUIZ71TO0.PNG)

![](image/2_MPbKb-xbzO.PNG)

![](image/5_hXjDSXZaC0.PNG)

![](image/3_6Gz3IxyZ3d.PNG)

## 2.AOP切面编程Demo示例

### 2.1导入pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <version>2.6.6</version>
</dependency>

```

### 2.2编写Aspect类

```java
@Component
@Aspect
public class DemoAspect {
    /**
      *第一个* ：方法的任何返回值
     * com.xmy.demonowcoder.service.*. *(..)) ：service包下的所有类所有方法所有参数(..)
     */
    @Pointcut("execution(* com.xmy.demonowcoder.service. *.*(..))")
    public void pointcut(){}

    /**切点方法之前执行(常用)**/
    @Before("pointcut()")
    public void before(){
        System.out.println("before");
    }
    @After("pointcut()")
    public void after(){
        System.out.println("after");
    }
    /**返回值以后执行**/
    @AfterReturning("pointcut()")
    public void afterRetuning() {
        System.out.println("afterRetuning");
    }

    /**抛出异常以后执行**/
    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }
    /**切点的前和后都可以执行**/
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("around before");
        Object obj = joinPoint.proceed();
        System.out.println("around after");
        return obj;
    }
}
```

## 3.AOP实现统一记录日志

**实现需求** ：用户ip地址\[1.2.3.4],在[xxx],访问了\[ **[com.nowcoder.community.service.xxx ](http://com.nowcoder.community.service.xxx "com.nowcoder.community.service.xxx")**()]业务.\\&#x20;

```java
@Component
@Aspect
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* com.xmy.demonowcoder.service.*. *(..))")
    public void pointcut(){}

    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        // 用户ip[1.2.3.4],在[xxx],访问了[com.nowcoder.community.service.xxx()].
        // 通过RequestContextHolder获取request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        // 通过request.getRemoteHost获取当前用户ip
        String ip = request.getRemoteHost();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        /**
         * joinPoint.getSignature().getDeclaringTypeName()-->com.nowcoder.community.service
         * joinPoint.getSignature().getName() -->方法名
         */
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." +joinPoint.getSignature().getName();
        // String.format()加工字符串
        logger.info(String.format("用户[%s],在[%s],访问了[%s]业务.", ip, time, target));
    }
}
```

# 项目监控（Springboot actuator）

## 1.引入pom.xml依赖

```xml
        <!-- actuator项目监控-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
            <version>2.7.0</version>
        </dependency>

```

## 2.配置yml文件

```yaml
# actuator项目监控
management:
  endpoints:
    web:
      exposure:
        include: beans,database,info,health
```

## 3.自定义监控id(database数据库监控)

```java
/**
 * QQ:260602448--xumingyu
 * 自定义项目监控类
 */
@Component
@Endpoint(id = "database")
public class DatabaseEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    @Autowired
    private DataSource dataSource;

    // 相当于GET请求
    @ReadOperation
    public String checkConnection() {
        try (
                // 放到try这个位置就不用释放资源，底层自动释放
                Connection conn = dataSource.getConnection();
        ) {
            return CommunityUtil.getJSONString(0, "获取连接成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败:" + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取连接失败!");
        }
    }}
```
## 4.使用SpringSecurity设置访问权限

```java
    .antMatchers(
            "/discuss/delete",
            "/data/* *",
            "/actuator/* *"
    )
    .hasAnyAuthority(
            AUTHORITY_ADMIN
    )
```

## 参考
  - https://blog.csdn.net/lijiaming_99/article/details/124931663
