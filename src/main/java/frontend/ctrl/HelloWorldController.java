package frontend.ctrl;

import com.doda2025_team14.lib_version.VersionUtil;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController {

    @GetMapping("/")
    @ResponseBody
    public String index() {
        return "Hello World!\nLibrary version: " + VersionUtil.getVersion();
    }
}