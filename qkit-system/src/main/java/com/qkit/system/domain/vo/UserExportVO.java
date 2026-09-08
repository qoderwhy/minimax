package com.qkit.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "用户导出 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportVO {

    @Schema(description = "用户名")
    @ExcelProperty("用户名")
    private String username;

    @Schema(description = "昵称")
    @ExcelProperty("昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    @ExcelProperty("真实姓名")
    private String realName;

    @Schema(description = "手机号")
    @ExcelProperty("手机号")
    private String phone;

    @Schema(description = "邮箱")
    @ExcelProperty("邮箱")
    private String email;

    @Schema(description = "部门")
    @ExcelProperty("部门")
    private String deptName;

    @Schema(description = "岗位")
    @ExcelProperty("岗位")
    private String postName;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String statusLabel;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
