package com.nt.service;

import java.util.List;

import com.nt.binding.ActivateUser;
import com.nt.binding.LoginCredential;
import com.nt.binding.UserAccount;

public interface IUserMgmtService {
	public String registerUser(UserAccount user) throws Exception;
	public String activateUserAccount(ActivateUser user);
	public String login(LoginCredential credentials);
	public List<UserAccount> listUsers();
	public UserAccount showUserByUserId(Integer id);
	public UserAccount showUserByEmailAndName(String email, String name);
	public String updateUser(UserAccount account);
	public String deleteUserById(Integer id);
	public String changeUserStatus(Integer id, String status);
	public String recoverPassword(String name, String email) throws Exception;

}
