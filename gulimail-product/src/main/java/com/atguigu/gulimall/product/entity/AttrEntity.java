package com.atguigu.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??Ʒ?
 * 
 * @author Ning
 * @email sszhangningwowow@gmail.com
 * @date 2022-07-18 09:56:06
 */
@Data
@TableName("pms_attr")
public class AttrEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ????id
	 */
	@TableId
	private Long attrId;
	/**
	 * ?????
	 */
	private String attrName;
	/**
	 * ?Ƿ???Ҫ????[0-????Ҫ??1-??Ҫ]
	 */
	private Integer searchType;
	/**
	 * ????ͼ?
	 */
	private String icon;
	/**
	 * ??ѡֵ?б?[?ö??ŷָ
	 */
	private String valueSelect;
	/**
	 * ????????[0-???????ԣ?1-???????ԣ?2-???????????????ǻ???????]
	 */
	private Integer attrType;
	/**
	 * ????״̬[0 - ???ã?1 - ????]
	 */
	private Long enable;
	/**
	 * ???????
	 */
	private Long catelogId;
	/**
	 * 快速展示「是否展示在介绍上」，在sku中仍然可以调整
	 */
	private Integer showDesc;

	private Integer valueType;



}
