package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("problem")
public class Problem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String sampleInput;
    private String sampleOutput;
    private String hint;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String difficulty;
    private String tags;
    private Long creatorId;
    private Integer status;
    @JsonIgnore
    private Integer acCount;
    private Integer submitCount;
    @TableField(exist = false)
    private Boolean solved;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @JsonProperty("acceptCount")
    public Integer getAcceptCount() {
        return acCount;
    }

    @JsonProperty("acceptCount")
    public void setAcceptCount(Integer acceptCount) {
        this.acCount = acceptCount;
    }

    @JsonProperty("passRate")
    public Integer getPassRate() {
        if (submitCount == null || submitCount <= 0 || acCount == null) {
            return 0;
        }
        return (int) Math.round(acCount * 100.0 / submitCount);
    }

    @JsonProperty("examples")
    public List<Example> getExamples() {
        if ((sampleInput == null || sampleInput.isBlank()) && (sampleOutput == null || sampleOutput.isBlank())) {
            return List.of();
        }
        Example example = new Example();
        example.setInput(sampleInput);
        example.setOutput(sampleOutput);
        return List.of(example);
    }

    @Data
    public static class Example {
        private String input;
        private String output;
    }
}
