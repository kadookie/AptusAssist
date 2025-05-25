import webpush from 'web-push';

const { publicKey, privateKey } = webpush.generateVAPIDKeys();

console.log('Public Key:\n' + publicKey);
console.log('Private Key:\n' + privateKey);
