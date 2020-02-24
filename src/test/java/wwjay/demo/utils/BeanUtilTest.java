package wwjay.demo.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wwj
 */
class BeanUtilTest {

    @Test
    void copyList() {
        ClassA a = new ClassA();
        a.setName("wwj");
        a.setAge(27);
        List<ClassA> listA = List.of(a);
        List<ClassB> listB = BeanUtil.copyList(listA, ClassB::new);
        assertTrue(() -> {
            ClassA classA = listA.get(0);
            ClassB classB = listB.get(0);
            return Objects.equals(classA.getName(), classB.getName()) &&
                    Objects.equals(classA.getAge(), classB.getAge());
        });
    }

    @Test
    void copyNotNullProperties() {
        ClassA a = new ClassA();
        a.setName("wwj");
        ClassB b = new ClassB();
        BeanUtil.copyNotNullProperties(a, b);
        assertAll(
                () -> assertEquals(b.getName(), a.getName()),
                () -> assertNull(b.getAge()));
    }

    @Test
    void copyNotEmptyProperties() {
        ClassA a = new ClassA();
        a.setName("");
        a.setAge(27);
        ClassB b = new ClassB();
        BeanUtil.copyNotEmptyProperties(a, b);
        assertAll(
                () -> assertNull(b.getName()),
                () -> assertEquals(a.getAge(), b.getAge()));
    }

    @Test
    void fieldsIsNull() {
        ClassA a = new ClassA();
        a.setName("wwj");
        a.setAge(27);
        ClassB b = new ClassB();
        assertAll(() -> assertFalse(BeanUtil.fieldsIsNull(a)),
                () -> assertTrue(BeanUtil.fieldsIsNull(b)));
    }

    @Getter
    @Setter
    @ToString
    private static class ClassA {

        private String name;
        private Integer age;
    }

    @Getter
    @Setter
    @ToString
    private static class ClassB {

        private String name;
        private Integer age;
    }
}