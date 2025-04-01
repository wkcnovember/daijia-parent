package com.atguigu.daijia.model.vo.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果包装
 *
 * @author itcast
 */
@Data
@Schema(description = "分页数据消息体")
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PageVo<T> implements Serializable {

    @Schema(description = "当前页码")
    private Long page;

    @Schema(description = "每页记录数")
    private Long limit;

    @Schema(description = "总页数")
    private Long pages;

    @Schema(description = "总条目数")
    private Long total;

    @Schema(description = "数据列表")
    private List<T> records;

    public PageVo(List<T> list, Long pages, Long total) {
        this.setRecords(list);
        this.setTotal(total);
        this.setPages(pages);
    }

    public static <E> PageVo<E> toPageVo(IPage<E> page) {
        PageVo<E> pageVo = new PageVo<>();
        pageVo.setPage(page.getCurrent());
        pageVo.setLimit(page.getSize());
        pageVo.setPages(page.getPages());
        pageVo.setTotal(page.getTotal());
        pageVo.setRecords(page.getRecords());
        return pageVo;
    }

}
