package com.olimpiici.arena.domain;

public class UserPoints implements Comparable<UserPoints> {
	public Long userId;
	public String firstName;
	public String lastName;
	public Integer points;

	public UserPoints(Long userId, String firstName, String lastName, Integer points) {
		super();
		this.userId = userId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.points = points;
	}

	public void setUser(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	@Override
	public int compareTo(UserPoints o) {
		return Integer.compare(o.points, this.points);
	}
}
