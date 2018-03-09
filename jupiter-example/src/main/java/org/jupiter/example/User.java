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

import org.jupiter.common.util.Lists;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class User implements Serializable {

    private long id;
    private String name;
    private int sex;
    private Date birthday;
    private String email;
    private String mobile;
    private String address;
    private List<Integer> permissions;
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

    public List<Integer> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Integer> permissions) {
        this.permissions = permissions;
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
        List<Integer> permsList = Lists.newArrayListWithCapacity(500);
        for (int i = 0; i < 200; i++) {
            permsList.add(i);
        }
        for (int i = 200; i < 350; i++) {
            permsList.add(Integer.MAX_VALUE - i);
        }
        for (int i = 350; i < 500; i++) {
            permsList.add(Integer.MIN_VALUE + i);
        }
        user.setPermissions(permsList);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return user;
    }
}