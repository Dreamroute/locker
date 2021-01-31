package com.github.dreamroute.locker.sample.domain;

import com.github.dreamroute.mybatis.pro.core.annotations.Id;
import com.github.dreamroute.mybatis.pro.core.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("smart_user")
public class User {

	@Id
	private Integer id;
	private String name;
	private String password;
	private Long version;


}
