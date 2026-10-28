/**
 * The public contract for code that uses a Bend client.
 *
 * <p>This package depends on no other library: it holds interfaces and small value types such as
 * {@link net.onestorm.bend.client.api.NamespacedId}. A user implements
 * {@link net.onestorm.bend.client.api.EndpointCodec} and
 * {@link net.onestorm.bend.client.api.EndpointHandler} to send and receive their own message types.
 * A client is created by an implementation's builder, which returns these interfaces.
 */
package net.onestorm.bend.client.api;
