/**
 * The public contract for code that uses a Bend client.
 *
 * <p>This package depends on no other library: it holds interfaces and small value types such as
 * {@link net.onestorm.bend.client.api.NamespacedId}. A user implements
 * {@link net.onestorm.bend.client.api.EndpointCodec} and
 * {@link net.onestorm.bend.client.api.EndpointHandler} to send and receive their own message types.
 * A client is created by an implementation's builder, which returns these interfaces.
 *
 * <p>Endpoints are registered on a {@link net.onestorm.bend.client.api.Client}, which returns an
 * {@link net.onestorm.bend.client.api.IncomingEndpoint} or
 * {@link net.onestorm.bend.client.api.OutgoingEndpoint}; both are an
 * {@link net.onestorm.bend.client.api.EndpointHandle} that can unregister the endpoint again.
 * Messages are sent with {@link net.onestorm.bend.client.api.OutgoingEndpoint#send(Object)}, which
 * throws an {@link IllegalStateException} when the endpoint can't be used right now. Whether an
 * endpoint can be used is reported by
 * {@link net.onestorm.bend.client.api.EndpointHandle#isAvailable()}, and changes to it go to the
 * {@link net.onestorm.bend.client.api.EndpointListener} given with the registration.
 *
 * <p>A {@link net.onestorm.bend.client.api.Client} reports whether it is connected, tells a
 * {@link net.onestorm.bend.client.api.ClientListener} when it connects and disconnects, with a
 * {@link net.onestorm.bend.client.api.DisconnectInfo} (its
 * {@link net.onestorm.bend.client.api.DisconnectOrigin} and
 * {@link net.onestorm.bend.client.api.DisconnectType}) saying why, and is shut down for good with
 * {@link net.onestorm.bend.client.api.Client#close()}.
 */
package net.onestorm.bend.client.api;
