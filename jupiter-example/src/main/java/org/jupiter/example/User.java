/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.example;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.jupiter.common.util.Lists;

public class User implements Serializable {

    private long id;
    private String name;
    private int sex;
    private Date birthday;
    private String email;
    private String mobile;
    private String address;
    private List<Long> permissions;
    private List<Integer> intList;
    private int status;
    private Date createTime;
    private Date updateTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Long> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Long> permissions) {
        this.permissions = permissions;
    }

    public List<Integer> getIntList() {
        return intList;
    }

    public void setIntList(List<Integer> intList) {
        this.intList = intList;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sex=" + sex +
                ", birthday=" + birthday +
                ", email='" + email + '\'' +
                ", mobile='" + mobile + '\'' +
                ", address='" + address + '\'' +
                ", permissions=" + permissions +
                ", intList=" + intList +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }

    public static User createUser() {
        User user = new User();
        user.setId(ThreadLocalRandom.current().nextInt());
        user.setName("block");
        user.setSex(0);
        user.setBirthday(new Date());
        user.setEmail("xxx@alibaba-inc.con");
        user.setMobile("18325038521");
        user.setAddress("浙江省 杭州市 文一西路969号");
        List<Long> permsList = Lists.newArrayList();
        permsList.add(1L);
        permsList.add(256L);
        permsList.add(256L * 256);
        permsList.add(256L * 256 * 256);
        permsList.add(256L * 256 * 256 * 256);
        permsList.add(256L * 256 * 256 * 256 * 256);
        permsList.add(256L * 256 * 256 * 256 * 256 * 256);
        permsList.add(256L * 256 * 256 * 256 * 256 * 256 * 128);
        permsList.add(256L * 256 * 256 * 256 * 256 * 256 * 256);
        permsList.add(-1L);
        permsList.add(-256L);
        user.setPermissions(permsList);
        List<Integer> integerList = Lists.newArrayList();
        integerList.add(1);
        integerList.add(2);
        integerList.add(3);
        integerList.add(256);
        integerList.add(256 * 256);
        integerList.add(256 * 256 * 256);
        integerList.add(256 * 256 * 256 * 64);
        integerList.add(Integer.MAX_VALUE);
        integerList.add(Integer.MAX_VALUE - 1);
        integerList.add(Integer.MAX_VALUE - 2);
        integerList.add(Integer.MIN_VALUE);
        integerList.add(Integer.MIN_VALUE + 1);
        integerList.add(Integer.MIN_VALUE + 2);
        integerList.add(-1);
        user.setIntList(integerList);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return user;
    }
}