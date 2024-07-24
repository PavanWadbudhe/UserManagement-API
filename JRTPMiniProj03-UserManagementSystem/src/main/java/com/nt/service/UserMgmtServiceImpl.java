package com.nt.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nt.binding.ActivateUser;
import com.nt.binding.LoginCredential;
import com.nt.binding.UserAccount;
import com.nt.constants.AppConstants;
import com.nt.entity.UserMaster;
import com.nt.properties.UserMgmtAppConfigProperties;
import com.nt.repository.IUserMasterRepository;
import com.nt.utils.EmailUtils;

@Service
public class UserMgmtServiceImpl implements IUserMgmtService {
	@Autowired
	private IUserMasterRepository userMasterRepo;
	@Autowired
	private Environment env;
	@Autowired
	private EmailUtils emailUtils;
	@Autowired
	private UserMgmtAppConfigProperties props;

	@Override
	@Transactional
	public String registerUser(UserAccount user) throws Exception {
		//conver UserAccount obj data to UserMaster obj(Entity obj) data
		UserMaster master=new UserMaster();
		BeanUtils.copyProperties(user, master);
		//set random string 6 char as password
		String tempPwd=generateRandomPassword(6);
		master.setPassword(tempPwd);
		master.setActive_sw(AppConstants.INACTIVE);
		//save obj
		UserMaster saveMaster=userMasterRepo.save(master);
		//send the mail
		String subject =props.getMessages().get(AppConstants.REGISTRATION_SUB);
		String body=readEmailMessageBody(env.getProperty(AppConstants.MAILBODY_REGISTERUSER_LOCATION), user.getName(), tempPwd);
		emailUtils.sendEmailMessage(user.getEmail(), subject, body);
		
		//return message
		return saveMaster!=null ? props.getMessages().get(AppConstants.REGISTER_SUCCESS)+saveMaster.getUserId() : props.getMessages().get(AppConstants.REGISTER_FAIL);
	}
	
	//helping method for same class
	private String generateRandomPassword(int length){
		//the list of characters choose from String 
		String alphaNumericStr=AppConstants.ALPHA_NUMERIC_STRING;
		//creating a StringBuffer of size alphaNumericStr password
		StringBuffer randomWord=new StringBuffer(length);
		for(int i=0; i<length; i++) {
			//generating the random number using Math.random()
			int ch=(int) (alphaNumericStr.length()*Math.random());
			//adding random character one by one at of randomWord
			randomWord.append(alphaNumericStr.charAt(ch));
		}
		return randomWord.toString();
	}
	
	private String readEmailMessageBody(String fileName, String fullName, String pwd) throws Exception {
		String mailBody=null;
		String url=props.getMessages().get(AppConstants.ACTIVATE_USER_URL);
		try (FileReader reader=new  FileReader(fileName);
				BufferedReader br=new BufferedReader(reader);){
			//read file data into StringBuffer object
			StringBuffer buffer=new StringBuffer();
			String line=null;
			
			do {
				line=br.readLine();
				if(line==null)
					break;
				buffer.append(line);
			}while(line != null);
			
			mailBody=buffer.toString();
			mailBody=mailBody.replace(AppConstants.FULL_NAME, fullName);
			mailBody=mailBody.replace(AppConstants.PWD, pwd);
			mailBody=mailBody.replace(AppConstants.URL, url);
		
		}catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
		return mailBody;
	}

	@Override
	public String activateUserAccount(ActivateUser user) {
		UserMaster entity=userMasterRepo.findByEmailAndPassword(user.getEmail() , user.getTempPassword());
		if(entity==null) {
			return props.getMessages().get(AppConstants.USER_ACTIVATION_FAIL);
		}else {
			//set the password
			entity.setPassword(user.getConfirmPassword());
			//change the acc status as active
			entity.setActive_sw(AppConstants.ACTIVE);
			//update the object
			userMasterRepo.save(entity);
			return entity.getUserId()+props.getMessages().get(AppConstants.USER_ACTIVATION_SUCCESS);
		}
	}

	@Override
	public String login(LoginCredential credentials) {
		//convert LoginCredential obj to UserMaster obj(Entity obj)
		UserMaster master=new UserMaster();
		BeanUtils.copyProperties(credentials, master);
		//prepare Example obj
		Example<UserMaster> example=Example.of(master);
		List<UserMaster> listEntities=userMasterRepo.findAll(example);
		if(listEntities.size()==0) {
			return props.getMessages().get(AppConstants.LOGIN_FAIL);
		}
		else {
			//get Entity obj
			UserMaster entity=listEntities.get(0);
			if(entity.getActive_sw().equalsIgnoreCase(AppConstants.ACTIVE)) {
				return props.getMessages().get(AppConstants.LOGIN_SUCCESS);
			}
			else 
				return props.getMessages().get(AppConstants.USER_ACC_NOT_ACTIVE);
		}
	}

	@Override
	public List<UserAccount> listUsers() {
		//use service
		List<UserAccount> listUsers=userMasterRepo.findAll().stream().map(entity->{
			UserAccount user=new UserAccount();
			BeanUtils.copyProperties(entity, user);
			return user;
		}).toList();
		return listUsers;
	}

	@Override
	public UserAccount showUserByUserId(Integer id) {
		//load the user by user id
		Optional<UserMaster> opt=userMasterRepo.findById(id);
		UserAccount account=null;
		if(opt.isPresent()) {
			 account=new UserAccount();
			BeanUtils.copyProperties(opt.get(), account);
		}
			return account;
	}

	@Override
	public UserAccount showUserByEmailAndName(String email, String name) {
		//use the custom findBy () method
		UserMaster master=userMasterRepo.findByEmailAndName(email, name);
		UserAccount account=null;
		if(master != null) {
			 account=new  UserAccount();
			 BeanUtils.copyProperties(master, account);
		}
		return account;
	}
	
	@Override
	public String updateUser(UserAccount account) {
		//use the  findBy () method
		Optional<UserMaster> opt=userMasterRepo.findById(account.getUserId());
		if(opt.isPresent()) {
			//get Entity obj
			UserMaster master=opt.get();
			BeanUtils.copyProperties(account, master);
			//save the user
			userMasterRepo.save(master);
			return props.getMessages().get(AppConstants.UPDATION_SUCCESS);
		}
		else
			return props.getMessages().get(AppConstants.UPDATION_FAIL);
	}

	@Override
	public String deleteUserById(Integer id) {
		//use repo
		Optional<UserMaster> opt=userMasterRepo.findById(id);
		if(opt.isPresent()) {
			userMasterRepo.deleteById(id);
			return props.getMessages().get(AppConstants.USER_DELETION_SECCESS);
		}
		else
			return props.getMessages().get(AppConstants.USER_DELETION_FAIL);
	}

	@Override
	public String changeUserStatus(Integer id, String status) {
		Optional<UserMaster> opt=userMasterRepo.findById(id);
		if(opt.isPresent()) {
			//get Entity obj
			UserMaster master=opt.get();
			master.setActive_sw(status);
			//save
			userMasterRepo.save(master);
			return props.getMessages().get(AppConstants.STATUSCHANGE_SUCCESS);
		}
		else
			return props.getMessages().get(AppConstants.STATUSCHANGE_FAIL);
	}

	@Override
	public String recoverPassword(String name, String email) throws Exception {
		//find user by email and name
		UserMaster master=userMasterRepo.findByEmailAndName(email, name);
		if(master !=null) {
			//get the user pwd
			String pwd=master.getPassword();
			//send pwd to  mail account
			String subject= props.getMessages().get(AppConstants.RECOVERPWD_SUB);
			String mailBody=readEmailMessageBody(env.getProperty(AppConstants.MAILBODY_RECOVERPWD_LOCATION), name, pwd);
			emailUtils.sendEmailMessage(email, subject, mailBody);
			return pwd;
		}
		return props.getMessages().get(AppConstants.RECOVERPWD_FAIL); 
	}
	
	

}
