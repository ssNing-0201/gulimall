package com.atguigu.gulimall.search.gulimallsearch.domain;

import lombok.Data;

@Data
public class User {
    private String name;
    private String age;
    private String gender;

    public User() {
    }

    public User(String name, String age, String gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

}
