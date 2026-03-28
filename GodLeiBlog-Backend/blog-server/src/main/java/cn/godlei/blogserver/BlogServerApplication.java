package cn.godlei.blogserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;


@ServletComponentScan({"cn.godlei.blogserver", "cn.godlei.blogcommon"})
@SpringBootApplication(scanBasePackages = {
        "cn.godlei.blogserver",
        "cn.godlei.blogcommon"
})
@MapperScan("cn.godlei.blogserver.mapper") // 扫描Mapper接口
public class BlogServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogServerApplication.class, args);
    }

}
