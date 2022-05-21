package com.jacknie.example.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

/**
 * Query DSL 사용 시 where clause 에 optional 하게 적용 되어야 할 조건들을 다루는 sugar code
 */
public abstract class QuerydslPredicateUtils {

    private QuerydslPredicateUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 값이 null 이면 쿼리 조건을 생성 하지 않는다.
     * @param mapper 쿼리 조건 매퍼
     * @param value 값
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifNullNone(Function<T, BooleanExpression> mapper, @Nullable T value) {
        if (value == null) {
            return null;
        } else {
            return mapper.apply(value);
        }
    }

    /**
     * 두 가지 값 모두 null 이면 조건을 생성하지 않는다.
     * @param mapper 쿼리 조건 매퍼
     * @param value1 값1
     * @param value2 값2
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifNullNone(BiFunction<T, T, BooleanExpression> mapper, @Nullable T value1, @Nullable T value2) {
        if (value1 == null && value2 == null) {
            return null;
        } else {
            return mapper.apply(value1, value2);
        }
    }

    /**
     * collection 의 각 구성들이 null 이 아니면 쿼리 조건을 생성하여 or/and 절로 합쳐 쿼리 조건을 생성한다.
     * @param mapper 쿼리 조건 매퍼
     * @param accumulator 쿼리 조건 누산 처리
     * @param collection 값 목록
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifNullNone(Function<T, BooleanExpression> mapper, BinaryOperator<BooleanExpression> accumulator, @Nullable Collection<T> collection) {
        return ifConditionalNone(mapper, Objects::isNull, accumulator, collection);
    }

    /**
     * 값이 empty 이면 쿼리 조건을 생성 하지 않는다.
     * @param mapper 쿼리 조건 매퍼
     * @param value 값
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifEmptyNone(Function<T, BooleanExpression> mapper, @Nullable T value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        } else {
            return mapper.apply(value);
        }
    }

    /**
     * 두 가지 값 모두 empty 이면 조건을 생성하지 않는다.
     * @param mapper 쿼리 조건 매퍼
     * @param value1 값1
     * @param value2 값2
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifEmptyNone(BiFunction<T, T, BooleanExpression> mapper, @Nullable T value1, @Nullable T value2) {
        if (ObjectUtils.isEmpty(value1) && ObjectUtils.isEmpty(value2)) {
            return null;
        } else {
            return mapper.apply(value1, value2);
        }
    }

    /**
     * collection 의 각 구성들이 empty 가 아니면 쿼리 조건을 생성하여 or/and 절로 합쳐 쿼리 조건을 생성한다.
     * @param mapper 쿼리 조건 매퍼
     * @param accumulator 쿼리 조건 누산 처리
     * @param collection 값 목록
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifEmptyNone(Function<T, BooleanExpression> mapper, BinaryOperator<BooleanExpression> accumulator, @Nullable Collection<T> collection) {
        return ifConditionalNone(mapper, ObjectUtils::isEmpty, accumulator, collection);
    }

    /**
     * 값이 true 이면 조건을 생성
     * @param expression 조건 생성 함수 객체
     * @param value 값
     * @return 쿼리 조건
     */
    @Nullable
    public static BooleanExpression ifFalseNone(Supplier<BooleanExpression> expression, @Nullable Boolean value) {
        if (value != null && value) {
            return expression.get();
        } else {
            return null;
        }
    }

    /**
     * collection 의 각 구성들이 predicate 조건에 해당 하지 않으면 쿼리 조건을 생성하여 or/and 절로 합쳐 쿼리 조건을 생성한다.
     * @param mapper 쿼리 조건 매퍼
     * @param predicate 쿼리 조건을 생성 하지 않는 조건
     * @param accumulator 쿼리 조건 누산 처리
     * @param collection 값 목록
     * @param <T> 값 타입
     * @return 쿼리 조건
     */
    @Nullable
    public static <T> BooleanExpression ifConditionalNone(
        Function<T, BooleanExpression> mapper,
        Predicate<T> predicate,
        BinaryOperator<BooleanExpression> accumulator,
        @Nullable Collection<T> collection
    ) {
        return Optional.ofNullable(collection)
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(predicate.negate())
            .map(mapper)
            .reduce(accumulator)
            .orElse(null);
    }

}

