package com.olimpiici.arena.domain;

public class UserPoints implements Comparable<UserPoints> {
	public User user;
	public Integer points;
	
	public UserPoints(User user, Integer points) {
		super();
		this.user = user;
		this.points = points;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
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
