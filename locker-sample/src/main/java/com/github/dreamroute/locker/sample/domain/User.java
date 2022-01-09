package com.github.dreamroute.locker.sample.domain;

import com.github.dreamroute.mybatis.pro.core.annotations.Id;
import com.github.dreamroute.mybatis.pro.core.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author w.dehai
 */
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

	public void setVersion(Long version) {
		this.version = version;
	}

}
