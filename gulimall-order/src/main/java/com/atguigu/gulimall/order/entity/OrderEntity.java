package com.atguigu.gulimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * ????
 * 
 * @author Ning
 * @email sszhangningwowow@gmail.com
 * @date 2022-07-18 20:51:09
 */
@Data
@TableName("oms_order")
public class OrderEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * member_id
	 */
	private Long memberId;
	/**
	 * ?????
	 */
	private String orderSn;
	/**
	 * ʹ?õ??Ż?ȯ
	 */
	private Long couponId;
	/**
	 * create_time
	 */
	private Date createTime;
	/**
	 * ?û??
	 */
	private String memberUsername;
	/**
	 * ?????ܶ
	 */
	private BigDecimal totalAmount;
	/**
	 * Ӧ???ܶ
	 */
	private BigDecimal payAmount;
	/**
	 * ?˷ѽ
	 */
	private BigDecimal freightAmount;
	/**
	 * ?????Ż?????????ۡ??????????ݼۣ?
	 */
	private BigDecimal promotionAmount;
	/**
	 * ???ֵֿ۽
	 */
	private BigDecimal integrationAmount;
	/**
	 * ?Ż?ȯ?ֿ۽
	 */
	private BigDecimal couponAmount;
	/**
	 * ??̨????????ʹ?õ??ۿ۽
	 */
	private BigDecimal discountAmount;
	/**
	 * ֧????ʽ??1->֧??????2->΢?ţ?3->?????? 4->?????????
	 */
	private Integer payType;
	/**
	 * ??????Դ[0->PC??????1->app????]
	 */
	private Integer sourceType;
	/**
	 * ????״̬??0->?????1->????????2->?ѷ?????3->?????ɣ?4->?ѹرգ?5->??Ч??????
	 */
	private Integer status;
	/**
	 * ??????˾(???ͷ?ʽ)
	 */
	private String deliveryCompany;
	/**
	 * ???????
	 */
	private String deliverySn;
	/**
	 * ?Զ?ȷ??ʱ?䣨?죩
	 */
	private Integer autoConfirmDay;
	/**
	 * ???Ի??õĻ
	 */
	private Integer integration;
	/**
	 * ???Ի??õĳɳ?ֵ
	 */
	private Integer growth;
	/**
	 * ??Ʊ????[0->??????Ʊ??1->???ӷ?Ʊ??2->ֽ?ʷ?Ʊ]
	 */
	private Integer billType;
	/**
	 * ??Ʊ̧ͷ
	 */
	private String billHeader;
	/**
	 * ??Ʊ???
	 */
	private String billContent;
	/**
	 * ??Ʊ?˵绰
	 */
	private String billReceiverPhone;
	/**
	 * ??Ʊ?????
	 */
	private String billReceiverEmail;
	/**
	 * ?ջ??????
	 */
	private String receiverName;
	/**
	 * ?ջ??˵绰
	 */
	private String receiverPhone;
	/**
	 * ?ջ????ʱ
	 */
	private String receiverPostCode;
	/**
	 * ʡ??/ֱϽ?
	 */
	private String receiverProvince;
	/**
	 * ???
	 */
	private String receiverCity;
	/**
	 * ?
	 */
	private String receiverRegion;
	/**
	 * ??ϸ??ַ
	 */
	private String receiverDetailAddress;
	/**
	 * ??????ע
	 */
	private String note;
	/**
	 * ȷ???ջ?״̬[0->δȷ?ϣ?1->??ȷ??]
	 */
	private Integer confirmStatus;
	/**
	 * ɾ??״̬??0->δɾ????1->??ɾ?
	 */
	private Integer deleteStatus;
	/**
	 * ?µ?ʱʹ?õĻ
	 */
	private Integer useIntegration;
	/**
	 * ֧??ʱ?
	 */
	private Date paymentTime;
	/**
	 * ????ʱ?
	 */
	private Date deliveryTime;
	/**
	 * ȷ???ջ?ʱ?
	 */
	private Date receiveTime;
	/**
	 * ????ʱ?
	 */
	private Date commentTime;
	/**
	 * ?޸?ʱ?
	 */
	private Date modifyTime;

	@TableField(exist = false)
	private List<OrderItemEntity> itemEntities;
}
