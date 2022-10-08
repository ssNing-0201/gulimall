package com.atguigu.gulimall.cart;

import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class GulimallCartApplicationTests {

    @Test
    void contextLoads() {
        CartItem cartItem = new CartItem();
        cartItem.setCount(2);
        cartItem.setPrice(new BigDecimal(131.00));
        List<CartItem> items = new ArrayList<>();
        items.add(cartItem);
        System.out.println(cartItem.getTotalPrice());
        Cart cart = new Cart();
        cart.setItems(items);
        System.out.println(cart.getCountNum());
        System.out.println(cart.getTotalAmount());
    }

}
