import { toast } from 'react-toastify';

export async function registerPush() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
    toast.warn('Push notifications not supported in your browser.');
    return;
  }

  try {
    const registration = await navigator.serviceWorker.ready;

    let subscription = await registration.pushManager.getSubscription();
if (!subscription) {
  subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(import.meta.env.VITE_VAPID_PUBLIC_KEY)
  });
}


    await fetch('/api/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subscription)
    });

    console.log('[Push] Subscribed:', subscription);
  } catch (err) {
    console.warn('[Push] Subscription failed:', err);
    toast.error('Failed to enable push notifications.');
  }
}

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = window.atob(base64);
  return new Uint8Array([...raw].map(char => char.charCodeAt(0)));
}
