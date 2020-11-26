package org.citopt.connde.domain.user_entity;

import java.util.ArrayList;
import java.util.List;

import org.citopt.connde.domain.env_model.EnvironmentModel;

public abstract class UserEntityRequestDTO {

    private EnvironmentModel environmentModelId;

    private List<String> accessControlPolicyIds = new ArrayList<>();
    
    // - - -

    public EnvironmentModel getEnvironmentModelId() {
		return environmentModelId;
	}
    
    public List<String> getAccessControlPolicyIds() {
		return accessControlPolicyIds;
	}
    
}