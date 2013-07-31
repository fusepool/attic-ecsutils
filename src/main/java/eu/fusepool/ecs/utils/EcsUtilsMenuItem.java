package eu.fusepool.ecs.utils;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

@Component
@Service(NavigationLink.class)
public class EcsUtilsMenuItem extends NavigationLink {
    
    public EcsUtilsMenuItem() {
        super("ecsutils/", "ECS Utils", "Utilities for the ECS", 300);
    }
    
}
