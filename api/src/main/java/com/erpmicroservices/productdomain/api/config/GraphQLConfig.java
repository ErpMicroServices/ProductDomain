package com.erpmicroservices.productdomain.api.config;

import com.erpmicroservices.productdomain.api.graphql.dataloader.CategoryBatchLoader;
import com.erpmicroservices.productdomain.api.graphql.dataloader.ProductBatchLoader;
import com.erpmicroservices.productdomain.api.graphql.dataloader.VariantBatchLoader;
import com.erpmicroservices.productdomain.api.graphql.exception.CustomDataFetcherExceptionResolver;
// import com.erpmicroservices.productdomain.api.graphql.scalar.DateTimeScalar;
// import com.erpmicroservices.productdomain.api.graphql.scalar.JsonScalar;
import com.erpmicroservices.productdomain.model.Category;
import com.erpmicroservices.productdomain.model.Product;
import com.erpmicroservices.productdomain.model.ProductVariant;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataLoaderRegistrar;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.util.UUID;

/**
 * GraphQL configuration for the Product Domain API.
 * Configures scalars, data loaders, and runtime wiring.
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.Json);
    }

    @Bean
    public DataLoaderRegistrar dataLoaderRegistrar(
            ProductBatchLoader productBatchLoader,
            CategoryBatchLoader categoryBatchLoader,
            VariantBatchLoader variantBatchLoader) {
        
        return (loaderRegistry, context) -> {
            // Product DataLoader
            DataLoader<UUID, Product> productLoader = DataLoaderFactory
                    .newDataLoader(productBatchLoader);
            loaderRegistry.register("productLoader", productLoader);

            // Category DataLoader
            DataLoader<UUID, Category> categoryLoader = DataLoaderFactory
                    .newDataLoader(categoryBatchLoader);
            loaderRegistry.register("categoryLoader", categoryLoader);

            // Variant DataLoader
            DataLoader<UUID, ProductVariant> variantLoader = DataLoaderFactory
                    .newDataLoader(variantBatchLoader);
            loaderRegistry.register("variantLoader", variantLoader);
        };
    }

    @Bean
    public CustomDataFetcherExceptionResolver customDataFetcherExceptionResolver() {
        return new CustomDataFetcherExceptionResolver();
    }

    @Bean
    public GraphQlSourceBuilderCustomizer graphQlSourceBuilderCustomizer() {
        return builder -> builder
                .configureRuntimeWiring(this::configureRuntimeWiring);
    }

    private RuntimeWiring.Builder configureRuntimeWiring(RuntimeWiring.Builder builder) {
        return builder;
                // .directive("auth", new AuthorizationDirective())
                // .directive("validate", new ValidationDirective());
    }
}