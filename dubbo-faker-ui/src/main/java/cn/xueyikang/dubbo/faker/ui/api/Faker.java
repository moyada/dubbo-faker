package cn.xueyikang.dubbo.faker.ui.api;

import cn.xueyikang.dubbo.faker.core.request.FakerRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.transform.Result;

/**
 * Created by xueyikang on 2017/12/22.
 */
@Api(value = "faker")
@Controller
@RequestMapping("/faker")
public class Faker {

    @Autowired
    private FakerRequest fakerRequest;

    @ApiOperation(value = "调用虚拟请求", httpMethod = "GET", produces = "application/json")
    @ApiResponse(code = 200, message = "success", response = Result.class)
    @ResponseBody
    @RequestMapping(value = "invoke", method = RequestMethod.GET, produces = "application/json")
    public String invoke(
                       @ApiParam(name = "invokeId", required = true, value = "请求编号", defaultValue = "1") @RequestParam("invokeId") int invokeId,
                       @ApiParam(name = "invokeExpression", required = true, value = "参数表达式", defaultValue = "[\"${1.model}\"]") @RequestParam("invokeExpression") String invokeExpression,
                       @ApiParam(name = "poolSize", value = "并发数") @RequestParam(value = "poolSize", required = false) Integer poolSize,
                       @ApiParam(name = "qps", value = "每秒钟请求数") @RequestParam(value = "qps", required = false) Integer qps,
                       @ApiParam(name = "loop", value = "请求次数") @RequestParam(value = "loop", required = false) Integer loop,
                       @ApiParam(name = "saveResult", value = "保存结果", defaultValue = "false") @RequestParam(value = "saveResult", required = false) Boolean saveResult,
                       @ApiParam(name = "resultParam", value = "选定保存结果参数") @RequestParam(value = "resultParam", required = false) String resultParam
                                ) {
        poolSize = null == poolSize || 1 > poolSize ? 1 : poolSize;
        qps = null == qps || 1 > qps ? 1 : qps;
        loop = null == loop || 1 > loop ? 1 : loop;
        if(loop < poolSize) {
            return "请求次数必须大于并发数";
        }
        if(loop < qps) {
            return "请求次数必须大于每秒钟请求数";
        }
        saveResult = null == saveResult ? false : saveResult;
        resultParam = null == resultParam || resultParam.trim().length() == 0 ? null : resultParam.trim();
        return fakerRequest.request(invokeId, invokeExpression, poolSize, qps, loop, saveResult, resultParam);
    }
}