package com.jacknie.example.config;

import com.jacknie.example.custom.CustomLookupStrategy;
import com.jacknie.example.custom.CustomMutableAclService;
import com.jacknie.example.custom.EnhancedMutableAclService;
import com.jacknie.example.custom.OperationsImpl;
import com.jacknie.example.repository.acl.AclClassRepository;
import com.jacknie.example.repository.acl.AclEntryRepository;
import com.jacknie.example.repository.acl.AclObjectIdentityRepository;
import com.jacknie.example.repository.acl.AclSidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final AclClassRepository classRepository;
    private final AclSidRepository sidRepository;
    private final AclObjectIdentityRepository oidRepository;
    private final AclEntryRepository entryRepository;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().requestMatchers(PathRequest.toH2Console());
    }

    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new AclPermissionEvaluator(aclService());
    }

    @Bean
    public EnhancedMutableAclService aclService() {
        AuditLogger auditLogger = new ConsoleAuditLogger();
        Cache cache = new ConcurrentMapCache("aclCache");
        PermissionGrantingStrategy permissionGrantingStrategy = new CumulativePermissionGrantingStrategy(auditLogger);
        AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority("ROLE_ADMIN"));
        AclCache aclCache = new SpringCacheBasedAclCache(cache, permissionGrantingStrategy, aclAuthorizationStrategy);
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(String.class, Long.class, Long::parseLong);
        OperationsImpl operations = new OperationsImpl(classRepository, sidRepository, oidRepository, entryRepository, conversionService);
        LookupStrategy lookupStrategy = new CustomLookupStrategy(operations, aclCache, aclAuthorizationStrategy, permissionGrantingStrategy);
        return new CustomMutableAclService(operations, lookupStrategy, aclCache);
    }

}
