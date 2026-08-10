package com.googlecode.xmemcached.extension.util;

/**
 * Tiny Spring-free string helpers used by {@code xmemcached-extension}.
 *
 * <p>Provides the single utility {@link #hasText(CharSequence)} which has semantics
 * equivalent to {@code org.springframework.util.StringUtils#hasText(CharSequence)}:
 * the argument is considered to contain text when it is non-{@code null}, has at
 * least one character, and contains at least one non-whitespace character.</p>
 *
 * <p>Implemented locally to keep {@code xmemcached-extension} free of any Spring
 * dependency.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see Character#isWhitespace(char)
 */
public abstract class Strings {

	/**
	 * Hidden utility constructor; this class is not meant to be instantiated.
	 */
	private Strings() {
	}

	/**
	 * Check whether the given {@code CharSequence} contains actual text.
	 *
	 * <p>More specifically, returns {@code true} if the {@code CharSequence} is not
	 * {@code null}, its length is greater than 0, and it contains at least one
	 * non-whitespace character.</p>
	 *
	 * @param str the {@code CharSequence} to check (may be {@code null})
	 * @return {@code true} if the {@code CharSequence} is not {@code null}, its
	 *         length is greater than 0, and it does not contain whitespace only
	 * @see Character#isWhitespace(char)
	 */
	public static boolean hasText(CharSequence str) {
		if (str == null) {
			return false;
		}
		int len = str.length();
		if (len == 0) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

}
