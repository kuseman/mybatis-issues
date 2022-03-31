package test;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleTest {

  private static SqlSessionFactory sqlSessionFactory;

  @BeforeClass
  public static void setUp() throws Exception {
    // create an SqlSessionFactory
    try (Reader reader = Resources.getResourceAsReader("test/mybatis-config.xml")) {
      sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    }
    // prepare in-memory database
    try (SqlSession session = sqlSessionFactory.openSession();
        Connection conn = session.getConnection();
        Reader reader = Resources.getResourceAsReader("test/CreateDB.sql")) {
      ScriptRunner runner = new ScriptRunner(conn);
      runner.setLogWriter(null);
      runner.runScript(reader);
    }
  }

  @Test
  public void shouldGetAUser() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user = mapper.getUser(1);
      assertEquals("User1", user.getName());
    }
  }

  @Test
  public void shouldInsertAUser() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user = new User();
      user.setId(3);
      user.setName("User3");
      mapper.insertUser(user);
      sqlSession.commit();
    }
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Mapper mapper = sqlSession.getMapper(Mapper.class);
      User user = mapper.getUser(3);
      assertEquals("User3", user.getName());
    }
  }

  @Test
  public void testQueryUsers() {
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
          Map<String, Object> map = new HashMap<>();
          map.put("returnedValue", -1);
          map.put("id", 2);

          List<User> users = sqlSession.selectList("queryUsers", map);
          assertEquals((Integer) 2, users.get(0).getId());
          assertEquals(0, map.get("returnedValue"));
      }
  }

  /**
   * JTDS
   * ----
   * If this test is executed with JTDS driver we get an exception
   * due to error:
   *   Error attempting to get column #1 from callable statement.  Cause: java.sql.SQLException: Output parameters have not yet been processed. Call getMoreResults().
   *
   * which is an effect of that JTDS somehow detects that we have more resultsets coming before 
   * we extract the returnValue the return statement
   *
   * This is a GOOD thing because then this call crashes and stops the "faulty" resultset from being
   * used upstream in mybatis that would cause tremendous damage because the system would treat the call
   * as successful
   *
   * MSSQL JDBC
   * ----------
   * If this is executed with MSSQL JDBC however it's silently executes which is a BAD thing for us
   * because of reasons above.
   * We can see this because the unit test fails when asserting that we got the correct user back
   *
   */
  @Test
  public void testQueryUsers_with_too_many_resultsets_back() {
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
          Map<String, Object> map = new HashMap<>();
          map.put("returnedValue", -1);
          map.put("id", 2);

          List<User> users = sqlSession.selectList("queryUsersWithDebugSelect", map);

          assertEquals((Integer) 2, users.get(0).getId());
          assertEquals(0, map.get("returnedValue"));
      }
  }
}
