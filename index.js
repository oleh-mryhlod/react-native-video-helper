import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
const { RNVideoHelper } = NativeModules;

const videoHelperEmitter = new NativeEventEmitter(RNVideoHelper);

const LISTENERS = '__listeners';

class ProgressPromise extends Promise {
  constructor(executor) {
    super((resolve, reject) => executor(resolve, reject,
      // Pass method for passing progress to listener
      value => {
        try {
          return this[LISTENERS].forEach(listener => listener(value));
        } catch(error) {
          reject(error);
        }
      }));
    this[LISTENERS] = [];
  }
  progress(handler) {
    if(typeof handler !== 'function')
      throw new Error('PROGRESS_REQUIRES_FUNCTION');
    this[LISTENERS].push(handler);
    return this;
  }
}

export default {
  compress: (source, options) => {
    return new ProgressPromise((resolve, reject, progress) => {
      const subscription = videoHelperEmitter.addListener('progress', p => progress(p));
  
      RNVideoHelper.compress(source, options).then(output => {
        const listeners = videoHelperEmitter.listeners('progress');
        if (listeners.length) {
          subscription.remove();
        }

        resolve(output);
      }).catch(err => reject(err));
    });
  },

  cancelCompress: () => {
    const listeners = videoHelperEmitter.listeners('progress');
    if (listeners.length) {
      videoHelperEmitter.removeAllListeners('progress');
    }

    if (Platform.OS === 'ios') return;
    
    RNVideoHelper.cancelCompress();
  },
}