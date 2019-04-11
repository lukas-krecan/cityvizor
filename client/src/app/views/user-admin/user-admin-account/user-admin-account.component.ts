import { Component, ViewChild, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { NgForm } from '@angular/forms';

import { AuthService } 		from '../../../services/auth.service';
import { DataService } 		from '../../../services/data.service';
import { ToastService } 		from '../../../services/toast.service';

import { User } from "../../../shared/schema/user";

//00006947
@Component({
	moduleId: module.id,
	selector: 'user-admin-account',
	templateUrl: 'user-admin-account.component.html',
	styleUrls: ['user-admin-account.component.scss']
})
export class UserAdminAccountComponent implements OnInit {

 	@ViewChild('userForm') userForm: NgForm;

  user:User;

	constructor(private authService:AuthService, private dataService: DataService, private toastService: ToastService) {
	}
   
  ngOnInit(){
    this.loadUser();
  }
	 
  loadUser(){
    this.dataService.getUser(this.authService.user._id)
      .then(user => this.user = user);
  }
   
	saveUser(form){
		
		let formData = form.value;
		formData._id = this.user._id;
		
		this.dataService.saveUser(formData)
			.then(user => {
				this.user = user;
				this.toastService.toast("Uloženo.","notice");
			})
			.catch(err => this.toastService.toast("Nastala chyba při ukládání dat.","error"));
	}
	 
	 savePass(form){
		 
		 let formData = form.value;
		 
		 if(formData.password !== formData.password2){
			 this.toastService.toast("Hesla se neshodují","notice");
			 return;
		 }
		 
		 let data = {
			 _id: this.user._id,
			 password: formData.password
		 };
		 
		 data._id = this.user._id;
		 
		 this.dataService.saveUser(data)
		 	.then(user => {
			 	form.reset();
				this.toastService.toast("Uloženo.","notice")
		 	})
			.catch(err => this.toastService.toast("Nastala chyba při ukládání dat.","error"));
	 }

}