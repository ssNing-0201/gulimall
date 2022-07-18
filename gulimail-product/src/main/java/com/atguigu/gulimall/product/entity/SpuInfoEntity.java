package com.atguigu.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu??Ϣ
 * 
 * @author Ning
 * @email sszhangningwowow@gmail.com
 * @date 2022-07-18 09:56:06
 */
@Data
@TableName("pms_spu_info")
public class SpuInfoEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ??Ʒid
	 */
	@TableId
	private Long id;
	/**
	 * ??Ʒ?
	 */
	private String spuName;
	/**
	 * ??Ʒ???
	 */
	private String spuDescription;
	/**
	 * ????????id
	 */
	private Long catalogId;
	/**
	 * Ʒ??id
	 */
	private Long brandId;
	/**
	 * 
	 */
	private BigDecimal weight;
	/**
	 * ?ϼ?״̬[0 - ?¼ܣ?1 - ?ϼ?]
	 */
	private Integer publishStatus;
	/**
	 * 
	 */
	private Date createTime;
	/**
	 * 
	 */
	private Date updateTime;

}
