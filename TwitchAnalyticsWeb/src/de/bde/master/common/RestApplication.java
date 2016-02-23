package de.bde.master.common;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import de.bde.master.bean.TwitchPrediction;

@ApplicationPath("/") 
public class RestApplication extends Application{
	@Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> set = new HashSet<Class<?>>(2);
        set.add(TwitchPrediction.class);
        set.add(CORSResponseFilter.class);
        
        return set;
    }
}
