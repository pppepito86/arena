package com.olimpiici.arena.service.util;

import java.util.Optional;

public class IntUtil {
	public static Optional<Integer> safeSum(Optional<Integer> a, Optional<Integer> b) {
		return Optional.of(safeSum(a.orElse(null), b.orElse(null)));
	}
	
	public static Integer safeSum(Integer a, Integer b) {
		Integer res = 0;
		if (a != null) res += a;
		if (b != null) res += b;
		return res;
	}
	
	public static Integer safeMax(Integer a, Integer b) {
		Integer max = Integer.MIN_VALUE;
		if (a != null) max = Math.max(max,  a);
		if (b != null) max = Math.max(max,  b);
		return max;
	}
}
