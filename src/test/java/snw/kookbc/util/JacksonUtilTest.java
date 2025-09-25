package snw.kookbc.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class JacksonUtilTest {

    @Test
    void testParseJson() {
        String json = "{\"name\":\"test\",\"age\":25,\"active\":true}";
        JsonNode node = JacksonUtil.parse(json);

        assertNotNull(node);
        assertTrue(JacksonUtil.has(node, "name"));
        assertTrue(JacksonUtil.has(node, "age"));
        assertTrue(JacksonUtil.has(node, "active"));
    }

    @Test
    void testGetAsString() {
        String json = "{\"name\":\"test\"}";
        JsonNode node = JacksonUtil.parse(json);

        assertEquals("test", JacksonUtil.getAsString(node, "name"));
    }

    @Test
    void testGetAsInt() {
        String json = "{\"age\":25}";
        JsonNode node = JacksonUtil.parse(json);

        assertEquals(25, JacksonUtil.getAsInt(node, "age"));
    }

    @Test
    void testGetAsLong() {
        String json = "{\"id\":12345678901}";
        JsonNode node = JacksonUtil.parse(json);

        assertEquals(12345678901L, JacksonUtil.getAsLong(node, "id"));
    }

    @Test
    void testGetAsDouble() {
        String json = "{\"price\":19.99}";
        JsonNode node = JacksonUtil.parse(json);

        assertEquals(19.99, JacksonUtil.getAsDouble(node, "price"), 0.001);
    }

    @Test
    void testGetAsBoolean() {
        String json = "{\"active\":true}";
        JsonNode node = JacksonUtil.parse(json);

        assertTrue(JacksonUtil.getAsBoolean(node, "active"));
    }

    @Test
    void testHas() {
        String json = "{\"name\":\"test\",\"value\":null}";
        JsonNode node = JacksonUtil.parse(json);

        assertTrue(JacksonUtil.has(node, "name"));
        assertFalse(JacksonUtil.has(node, "value"));  // null value
        assertFalse(JacksonUtil.has(node, "notexist"));  // non-existent key
    }

    @Test
    void testGetThrowsExceptionForMissingField() {
        String json = "{\"name\":\"test\"}";
        JsonNode node = JacksonUtil.parse(json);

        assertThrows(NoSuchElementException.class, () -> {
            JacksonUtil.get(node, "notexist");
        });
    }

    @Test
    void testGetThrowsExceptionForNullField() {
        String json = "{\"value\":null}";
        JsonNode node = JacksonUtil.parse(json);

        assertThrows(NoSuchElementException.class, () -> {
            JacksonUtil.get(node, "value");
        });
    }

    @Test
    void testToJson() {
        TestObject obj = new TestObject("test", 25);
        String json = JacksonUtil.toJson(obj);

        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"test\""));
        assertTrue(json.contains("\"age\""));
        assertTrue(json.contains("25"));
    }

    @Test
    void testFromJsonClass() {
        String json = "{\"name\":\"test\",\"age\":25}";
        TestObject obj = JacksonUtil.fromJson(json, TestObject.class);

        assertNotNull(obj);
        assertEquals("test", obj.getName());
        assertEquals(25, obj.getAge());
    }

    static class TestObject {
        private String name;
        private int age;

        public TestObject() {}

        public TestObject(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}