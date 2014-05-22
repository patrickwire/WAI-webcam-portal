package dao;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import exception.CamNotFoundException;
import exception.CamNotSavedException;
import jndi.JndiFactory;
import model.Cam;
import model.User;

public class UserDaoImpl implements UserDao {
	
	final JndiFactory jndi = JndiFactory.getInstance();
	
	@Override
	public void save(User user) {
		if (user == null)
			throw new IllegalArgumentException("user can not be null");
		
		Connection connection = null;	
		
		final int ITERATION = 999;
		String saltValue = new BigInteger(10, new SecureRandom()).toString(5);
		String password = user.getPassword();
		user.setSaltValue(saltValue);
		
		if (!password.isEmpty()){
		
			for (int i = 0; i < ITERATION; i++) {
				password = sha256(password+saltValue);
			}
			
			user.setPassword(password);
			
		}
		
		try {
			connection = jndi.getConnection("jdbc/postgres");
			PreparedStatement pstmt;
			if (user.getId() == null) {
				pstmt = connection.prepareStatement("insert into \"user\" (username, password, saltvalue) values (?,?,?)");
				pstmt.setString(3, user.getSaltValue());
				pstmt.setString(2, user.getPassword());
			} else {
				if (!user.getPassword().trim().isEmpty()) {
					pstmt = connection.prepareStatement("update \"user\"  set username = ?, password = ?, saltvalue = ? where id = ?");
					pstmt.setString(2, user.getPassword());
					pstmt.setString(3, user.getSaltValue());
					pstmt.setLong(4, user.getId());
				} else {
					pstmt = connection.prepareStatement("update \"user\"  set username = ? where id = ?");
					pstmt.setLong(2, user.getId());
				}
				
			}
			pstmt.setString(1, user.getUsername());
			pstmt.executeUpdate();
		} catch (Exception e) {
			throw new CamNotSavedException();
		} finally {
			closeConnection(connection);
		}		
	}

	@Override
	public User getUser(Long id) {
		if (id == null)
			throw new IllegalArgumentException("id can not be null");
		
		Connection connection = null;		
		try {
			connection = jndi.getConnection("jdbc/postgres");			
			PreparedStatement pstmt = connection.prepareStatement("select * from \"user\" where id = ?");
			pstmt.setLong(1, id);
			ResultSet rs = pstmt.executeQuery();							
			if (rs.next()) {
				User user = new User();
				user.setId(rs.getLong("id"));
				user.setUsername(rs.getString("username"));
				user.setPassword(rs.getString("password"));
				return user;
			} else {
				throw new CamNotFoundException(id);
			}			
		} catch (Exception e) {
			throw new CamNotFoundException(id);
		} finally {	
			closeConnection(connection);
		}
	}

	@Override
	public void deleteUser(Long id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<User> list() {
		
	List<User> userList = new ArrayList<User>();
		
		Connection connection = null;		
		try {
			connection = jndi.getConnection("jdbc/postgres");			
			
				PreparedStatement pstmt = connection.prepareStatement("select * from \"user\" order by id asc");				
				ResultSet rs = pstmt.executeQuery();
								
				while (rs.next()) {
					User user = new User();
					user.setId(rs.getLong("id"));
					user.setUsername(rs.getString("username"));
					user.setPassword(rs.getString("password"));
					user.setSaltValue(rs.getString("saltValue"));
					userList.add(user);
				}			
			
			return userList;
			
		} catch (Exception e) {
			throw new CamNotFoundException();
		} finally {	
			closeConnection(connection);
		}
	}
	
	private void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
				connection = null;
			} catch (SQLException e) {
				// nothing to do
				e.printStackTrace();
			}				
		}
	}
	
	private String sha256(String base) {
	    try{
	        MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        byte[] hash = digest.digest(base.getBytes("UTF-8"));
	        StringBuffer hexString = new StringBuffer();

	        for (int i = 0; i < hash.length; i++) {
	            String hex = Integer.toHexString(0xff & hash[i]);
	            if(hex.length() == 1) hexString.append('0');
	            hexString.append(hex);
	        }

	        return hexString.toString();
	    } catch(Exception ex){
	       throw new RuntimeException(ex);
	    }
	}
	
}
