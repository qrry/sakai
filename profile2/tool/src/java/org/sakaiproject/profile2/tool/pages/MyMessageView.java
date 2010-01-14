/**
 * Copyright (c) 2008-2010 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.profile2.tool.pages;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.profile2.model.Message;
import org.sakaiproject.profile2.model.MessageParticipant;
import org.sakaiproject.profile2.tool.components.ProfileImageRenderer;
import org.sakaiproject.profile2.tool.dataproviders.MessagesDataProvider;
import org.sakaiproject.profile2.tool.models.StringModel;
import org.sakaiproject.profile2.tool.pages.panels.ConfirmedFriends;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.sakaiproject.profile2.util.ProfileUtils;

public class MyMessageView extends BasePage {
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ConfirmedFriends.class);
	
	public MyMessageView(final String currentUserUuid, final long threadId, final String threadSubject) {
		
		log.debug("MyMessageView()");
		
		//get API's
		sakaiProxy = getSakaiProxy();
		profileLogic = getProfileLogic();
		
		//buttons
		Form<Void> buttonsForm = new Form<Void>("buttonsForm");
		
		//backbutton
		Button backButton = new Button("backButton", new ResourceModel("button.message.backtolist")) {
			private static final long serialVersionUID = 1L;
			public void onSubmit() {
				setResponsePage(new MyMessageThreads());
			}
		};
		backButton.setDefaultFormProcessing(false);
		buttonsForm.add(backButton);
		add(buttonsForm);
		
		//details container
		WebMarkupContainer messageDetailsContainer = new WebMarkupContainer("messageDetailsContainer");
		messageDetailsContainer.setOutputMarkupId(true);
		
		//thread subject
		Label threadSubjectLabel = new Label("threadSubject", new Model<String>(threadSubject));
		messageDetailsContainer.add(threadSubjectLabel);
		
		add(messageDetailsContainer);
		
		//list container
		final WebMarkupContainer messageListContainer = new WebMarkupContainer("messageListContainer");
		messageListContainer.setOutputMarkupId(true);
		
		
		//get our list of messages
		final MessagesDataProvider provider = new MessagesDataProvider(threadId);
		
		DataView<Message> messageList = new DataView<Message>("messageList", provider) {
			private static final long serialVersionUID = 1L;

			protected void populateItem(final Item<Message> item) {
		        
				final Message message = (Message)item.getDefaultModelObject();
				final String messageFromUuid = message.getFrom();
				
				//we need to know if this message has been read or not so we can style it accordingly
				//we only need this if we didn't send the message
				MessageParticipant participant = null;
				
				boolean messageOwner = false;
				if(StringUtils.equals(messageFromUuid, currentUserUuid)) {
					messageOwner = true;
				}
				if(!messageOwner) {
					participant = profileLogic.getMessageParticipant(message.getId(), currentUserUuid);
				}
				
				
				//friend?
				boolean friend = profileLogic.isUserXFriendOfUserY(messageFromUuid, currentUserUuid);
				
				//photo link
				AjaxLink<String> photoLink = new AjaxLink<String>("photoLink", new Model<String>(messageFromUuid)) {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						setResponsePage(new ViewProfile(getModelObject()));
					}
					
				};
				
				//photo
				photoLink.add(new ProfileImageRenderer("messagePhoto", messageFromUuid, profileLogic.isUserXProfileImageVisibleByUserY(messageFromUuid, currentUserUuid, friend), ProfileConstants.PROFILE_IMAGE_THUMBNAIL, false));
				item.add(photoLink);
				
				
				//name link
				AjaxLink<String> messageFromLink = new AjaxLink<String>("messageFromLink", new Model<String>(messageFromUuid)) {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						setResponsePage(new ViewProfile(getModelObject()));
					}
					
				};
				messageFromLink.add(new Label("messageFromName", new Model<String>(sakaiProxy.getUserDisplayName(messageFromUuid))));
				item.add(messageFromLink);
			
				//date
				item.add(new Label("messageDate", ProfileUtils.convertDateToString(message.getDatePosted(), ProfileConstants.MESSAGE_DISPLAY_DATE_FORMAT)));
				
				//message body
				item.add(new Label("messageBody", new Model<String>(message.getMessage())));
				
				//highlight if new, then mark it as read
				if(!messageOwner && !participant.isRead()) {
					item.add(new AttributeAppender("class", true, new Model<String>("unread-message"), " "));
					//profileLogic.toggleMessageRead(message, true);
				}
				
				item.setOutputMarkupId(true);
				
		    }
			
		};
		messageList.setOutputMarkupId(true);
		messageListContainer.add(messageList);
		add(messageListContainer);
		
		//reply form
		StringModel stringModel = new StringModel();
		Form<StringModel> replyForm = new Form<StringModel>("replyForm", new Model<StringModel>(stringModel));
		
		//form feedback
		final Label formFeedback = new Label("formFeedback");
		formFeedback.setOutputMarkupPlaceholderTag(true);
		add(formFeedback);
		
		//reply field
		replyForm.add(new Label("replyLabel", new ResourceModel("message.reply")));
		final TextArea<StringModel> replyField = new TextArea<StringModel>("replyField", new PropertyModel<StringModel>(stringModel, "string"));
		replyField.setRequired(true);
		replyField.setOutputMarkupId(true);
		replyForm.add(replyField);
		
		//reply button
		IndicatingAjaxButton replyButton = new IndicatingAjaxButton("replyButton", replyForm) {
			private static final long serialVersionUID = 1L;

			protected void onSubmit(AjaxRequestTarget target, Form form) {
				
        		StringModel stringModel = (StringModel) form.getModelObject();
        		String reply = stringModel.getString();
				
        		//send it
        		if(profileLogic.replyToThread(threadId, reply, currentUserUuid)) {
        			//clear this field
        			replyField.setModelObject(null);
        			target.addComponent(replyField);
        			
        			//repaint the list of messages in this thread
        			//target.addComponent(messageListContainer);
        		}
        		
        		// ...
				
				
				/*
				if(true) {
					//success
					formFeedback.setDefaultModel(new ResourceModel("success.message.send.ok"));
					formFeedback.add(new AttributeModifier("class", true, new Model<String>("success")));
				} else {
					//error
					formFeedback.setDefaultModel(new ResourceModel("error.message.send.failed"));
					formFeedback.add(new AttributeModifier("class", true, new Model<String>("alert")));
				}
				*/
				
				
            }
			
			protected void onError(AjaxRequestTarget target, Form form) {
				
				//validate
				if(!replyField.isValid()) {
					formFeedback.setDefaultModel(new ResourceModel("error.message.required.body"));
				}
				formFeedback.add(new AttributeModifier("class", true, new Model<String>("alertMessage")));	
				target.addComponent(formFeedback);
			}
		};
		replyForm.add(replyButton);
		replyButton.setModel(new ResourceModel("button.message.send"));
		
		add(replyForm);

		
		
		
	}
	
	


	/* reinit for deserialisation (ie back button) */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		log.debug("MessageList has been deserialized.");
		//re-init our transient objects
		profileLogic = getProfileLogic();
		sakaiProxy = getSakaiProxy();
	}
	
}
