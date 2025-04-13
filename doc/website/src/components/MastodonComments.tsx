import React, { useEffect, useRef } from 'react';

interface MastodonCommentsProps {
  host: string;
  user: string;
  tootId: String;
}

const MastodonComments: React.FC<MastodonCommentsProps> = ({
  tootId,
  host = 'mastodon.ml',
  user = 'littlesavage',
}) => {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!customElements.get('markdown-comments')) {
      const domPurifyScript = document.createElement('script')
      domPurifyScript.src = 'https://cdnjs.cloudflare.com/ajax/libs/dompurify/2.4.1/purify.min.js'
      domPurifyScript.integrity = 'sha512-uHOKtSfJWScGmyyFr2O2+efpDx2nhwHU2v7MVeptzZoiC7bdF6Ny/CmZhN2AwIK1oCFiVQQ5DA/L9FSzyPNu6Q=='
      domPurifyScript.crossorigin = 'anonymous'
      domPurifyScript.referrerpolicy = 'no-referrer'
      domPurifyScript.type = 'module'
      domPurifyScript.async = true
      document.head.appendChild(domPurifyScript)

      const commentsScript = document.createElement('script')
      commentsScript.src = '/js/mastodon-comments.js'
      commentsScript.type = 'module'
      commentsScript.async = true
      document.head.appendChild(commentsScript)
    }
  }, []);

  return (
    <div ref={ref} className="markdown-comments-container" style={{ marginTop: '1rem' }}>
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"/>
      <mastodon-comments host={host} user={user} tootId={tootId} />
    </div>
  );
};

export default MastodonComments;
