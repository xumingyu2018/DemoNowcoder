// 上传到七牛云服务器的异步处理方法
$(function () {
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
        success: function (data) {
            if (data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName": $("input[name='key']").val()},
                    function (data) {
                        data = $.parseJSON(data);
                        if (data.code == 0) {
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