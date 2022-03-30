package test;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Reader;
import java.sql.Connection;
import java.util.List;

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
          List<User> users = sqlSession.selectList("queryUsers", singletonMap("id", 2));
          assertEquals((Integer) 2, users.get(0).getId());
      }
  }

  /**
   * Here we query a procedure that returns too many result sets due to some
   * unexpected select inside a stored procedure.
   *
   * I think this should fail.
   *
   * In org.apache.ibatis.executor.resultset.DefaultResultSetHandler.handleResultSets(Statement)
   *
   * at line 200
   *
   * we have 'rsw != null' which indicates that we did get back a result set even when
   * resultMapCount > resultSetCount was full filled and I think an exception should be thrown
   * when that happens.
   *
   */
  @Test
  public void testQueryUsers_with_too_many_resultsets_back() {
      try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
          sqlSession.selectList("queryUsersWithDebugSelect", singletonMap("id", 2));
          fail("Should fail becuase we have 2 resultsets returned but mapping only has one specified" );
      }
  }
}
