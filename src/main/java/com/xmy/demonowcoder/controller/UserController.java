package com.xmy.demonowcoder.controller;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.xmy.demonowcoder.annotation.LoginRequired;
import com.xmy.demonowcoder.entities.Comment;
import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.Page;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.service.*;
import com.xmy.demonowcoder.util.CommunityConstant;
import com.xmy.demonowcoder.util.CommunityUtil;
import com.xmy.demonowcoder.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xumingyu
 * @date 2022/4/28
 **/
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    /**获得当前登录用户的信息**/
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    // 头像上传到云服务器
    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

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


    /**
     * 上传头像功能（废弃：用云服务器替代）
     **/
    @LoginRequired
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
//        StringUtils.isBlank(headerImage)
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }
        /*
         * 获得原始文件名字
         * 目的是：生成随机不重复文件名，防止同名文件覆盖
         * 方法：获取.后面的图片类型 加上 随机数
         * */
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));

        //任何文件都可以上传,根据业务在此加限制
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        //生成随机文件名
        filename = CommunityUtil.generateUUID() + suffix;
        //确定文件存放路劲
        File dest = new File(uploadPath + "/" + filename);
        try {
            //将文件存入指定位置
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败： " + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }
        //更新当前用户的头像的路径（web访问路径）
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    /**
     * 获取头像（废弃：用云服务器替代）
     **/
    @RequestMapping(value = "/header/{fileName}", method = RequestMethod.GET)
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

    /**
     * 修改密码
     **/
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map == null || map.isEmpty()) {
            /**如果更改密码成功，退出登录，并跳到登录页面**/
            return "redirect:/logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }

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
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

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

    /**
     * 查询我的帖子
     */
    @RequestMapping(value = "/myPost/{userId}", method = RequestMethod.GET)
    public String getMyDiscussPost(@PathVariable("userId") int userId, Model model, Page page) {
        page.setLimit(5);
        page.setPath("/user/myPost/" + userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));

        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("rows", discussPostService.findDiscussPostRows(userId));

        // 按时间排序
        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String, Object>> discussPost = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                discussPost.add(map);
            }
        }
        model.addAttribute("discussPost", discussPost);

        return "/site/my-post";
    }

    /**
     * 查询我的回复
     */
    @RequestMapping(value = "/myComment/{userId}", method = RequestMethod.GET)
    public String getMyComment(@PathVariable("userId") int userId, Model model, Page page) {

        int count = commentService.findCommentCountByUserId(userId, ENTITY_TYPE_POST);
        page.setLimit(5);
        page.setPath("/user/myComment/" + userId);
        page.setRows(count);
        model.addAttribute("count", count);

        User user = userService.findUserById(userId);
        model.addAttribute("user", user);

        List<Comment> list = commentService.findCommentsByUserId(userId, ENTITY_TYPE_POST, page.getOffset(), page.getLimit());
        List<Map<String, Object>> comments = new ArrayList<>();
        if (list != null) {
            for (Comment comment : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);

                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("post", post);

                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);

        return "/site/my-reply";
    }
}
