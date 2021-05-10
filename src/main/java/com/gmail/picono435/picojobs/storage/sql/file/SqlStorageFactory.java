package com.gmail.picono435.picojobs.storage.sql.file;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import com.gmail.picono435.picojobs.storage.StorageFactory;

public abstract class SqlStorageFactory extends StorageFactory {
	
	protected ConfigurationSection configurationSection;
	protected Connection conn;
	
	@Override
	protected void destroyStorage() {
		try {
			this.conn.close();
		} catch(Exception ex) {
			System.out.println("Error connecting to the storage. The plugin will not work correctly.");
			return;
		}
	}
	
	@Override
	public String getJob(UUID uuid) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("SELECT `job` FROM ? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setString(2, uuid.toString());
        	ResultSet rs = stm.executeQuery();
        	stm.close();
        	conn.close();
        	if(rs.next()) {
        		return rs.getString("job");
        	} else {
        		return null;
        	}
		}
	}

	@Override
	public double getMethod(UUID uuid) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("SELECT `method` FROM ? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setString(2, uuid.toString());
        	ResultSet rs = stm.executeQuery();
        	stm.close();
        	conn.close();
        	if(rs.next()) {
        		return rs.getDouble("method");
        	} else {
        		return 0;
        	}
		}
	}

	@Override
	public double getMethodLevel(UUID uuid) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("SELECT `level` FROM ? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setString(2, uuid.toString());
        	ResultSet rs = stm.executeQuery();
        	stm.close();
        	conn.close();
        	if(rs.next()) {
        		return rs.getDouble("level");
        	} else {
        		return 0;
        	}
		}
	}

	@Override
	public boolean isWorking(UUID uuid) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("SELECT `is-working` FROM ? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setString(2, uuid.toString());
        	ResultSet rs = stm.executeQuery();
        	stm.close();
        	conn.close();
        	if(rs.next()) {
        		return rs.getBoolean("is-working");
        	} else {
        		return false;
        	}
		}
	}

	@Override
	public double getSalary(UUID uuid) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("SELECT `salary` FROM ? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setString(2, uuid.toString());
        	ResultSet rs = stm.executeQuery();
        	stm.close();
        	conn.close();
        	if(rs.next()) {
        		return rs.getDouble("salary");
        	} else {
        		return 0;
        	}
		}
	}

	@Override
	public boolean setJob(UUID uuid, String job) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("UPDATE ? SET `job`=? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setString(2, job);
        	stm.setString(3, uuid.toString());
        	int result = stm.executeUpdate();
        	stm.close();
        	conn.close();
        	return result >= 1;
		}
	}

	@Override
	public boolean setMethod(UUID uuid, double method) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("UPDATE ? SET `method`=? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setDouble(2, method);
        	stm.setString(3, uuid.toString());
        	int result = stm.executeUpdate();
        	stm.close();
        	conn.close();
        	return result >= 1;
		}
	}

	@Override
	public boolean setMethodLevel(UUID uuid, double level) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("UPDATE ? SET `level`=? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setDouble(2, level);
        	stm.setString(3, uuid.toString());
        	int result = stm.executeUpdate();
        	stm.close();
        	conn.close();
        	return result >= 1;
		}
	}

	@Override
	public boolean setWorking(UUID uuid, boolean isWorking) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("UPDATE ? SET `is-working`=? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setBoolean(2, isWorking);
        	stm.setString(3, uuid.toString());
        	int result = stm.executeUpdate();
        	stm.close();
        	conn.close();
        	return result >= 1;
		}
	}

	@Override
	public boolean setSalary(UUID uuid, double salary) throws Exception {
		try(Connection conn = this.conn) {
			PreparedStatement stm = conn.prepareStatement("UPDATE ? SET `salary`=? WHERE `uuid`=?");
        	stm.setString(1, configurationSection.getString("tablename"));
        	stm.setDouble(2, salary);
        	stm.setString(3, uuid.toString());
        	int result = stm.executeUpdate();
        	stm.close();
        	conn.close();
        	return result >= 1;
		}
	}
	
}
